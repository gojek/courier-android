package com.gojek.courier.coordinator

import com.gojek.courier.Message
import com.gojek.courier.MessageAdapter
import com.gojek.courier.QoS
import com.gojek.courier.logging.ILogger
import com.gojek.courier.stub.StubInterface
import com.gojek.courier.stub.StubMethod
import com.gojek.courier.utils.toStream
import com.gojek.mqtt.client.MqttClient
import com.gojek.mqtt.client.listener.MessageListener
import com.gojek.mqtt.client.model.MqttMessage
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.FlowableOnSubscribe
import io.reactivex.schedulers.Schedulers

internal class Coordinator(
    private val client: MqttClient,
    private val logger: ILogger
) : StubInterface.Callback {

    @Synchronized
    override fun send(stubMethod: StubMethod.Send, args: Array<Any>): Any {
        val data = stubMethod.argumentProcessor.getDataArgument(args)
        stubMethod.argumentProcessor.inject(args)
        val topic = stubMethod.argumentProcessor.getTopic()
        val message = stubMethod.messageAdapter.toMessage(topic, data)
        return client.send(message, topic, stubMethod.qos)
    }

    @Synchronized
    override fun receive(stubMethod: StubMethod.Receive, args: Array<Any>): Any {
        stubMethod.argumentProcessor.inject(args)
        val topic = stubMethod.argumentProcessor.getTopic()

        val flowable = Flowable.create(
            FlowableOnSubscribe<MqttMessage> { emitter ->
                val listener = object : MessageListener {
                    override fun onMessageReceived(mqttMessage: MqttMessage) {
                        if (emitter.isCancelled.not()) {
                            emitter.onNext(mqttMessage)
                        }
                    }
                }
                client.addMessageListener(topic, listener)
                emitter.setCancellable { client.removeMessageListener(topic, listener) }
            },
            BackpressureStrategy.BUFFER
        )

        val stream = flowable
            .observeOn(Schedulers.computation())
            .flatMap { mqttMessage ->
                mqttMessage.message.adapt(
                    mqttMessage.topic,
                    stubMethod.messageAdapter
                )?.let { Flowable.just(it) } ?: Flowable.empty()
            }
            .toStream()
        return stubMethod.streamAdapter.adapt(stream)
    }

    override fun subscribe(stubMethod: StubMethod.Subscribe, args: Array<Any>): Any {
        stubMethod.argumentProcessor.inject(args)
        val topic = stubMethod.argumentProcessor.getTopic()
        return client.subscribe(topic to stubMethod.qos)
    }

    override fun subscribeWithStream(
        stubMethod: StubMethod.SubscribeWithStream,
        args: Array<Any>
    ): Any {
        stubMethod.argumentProcessor.inject(args)
        val topic = stubMethod.argumentProcessor.getTopic()
        client.subscribe(topic to stubMethod.qos)

        val flowable = Flowable.create(
            FlowableOnSubscribe<MqttMessage> { emitter ->
                val listener = object : MessageListener {
                    override fun onMessageReceived(mqttMessage: MqttMessage) {
                        if (emitter.isCancelled.not()) {
                            emitter.onNext(mqttMessage)
                        }
                    }
                }
                client.addMessageListener(topic, listener)
                emitter.setCancellable { client.removeMessageListener(topic, listener) }
            },
            BackpressureStrategy.BUFFER
        )

        val stream = flowable
            .observeOn(Schedulers.computation())
            .flatMap { mqttMessage ->
                mqttMessage.message.adapt(
                    mqttMessage.topic,
                    stubMethod.messageAdapter
                )?.let { Flowable.just(it) } ?: Flowable.empty()
            }
            .toStream()
        return stubMethod.streamAdapter.adapt(stream)
    }

    override fun unsubscribe(stubMethod: StubMethod.Unsubscribe, args: Array<Any>): Any {
        stubMethod.argumentProcessor.inject(args)
        val topics = stubMethod.argumentProcessor.getTopics()
        return if (topics.size == 1) {
            client.unsubscribe(topics[0])
        } else {
            client.unsubscribe(topics[0], *topics.sliceArray(IntRange(1, topics.size - 1)))
        }
    }

    override fun subscribeAll(stubMethod: StubMethod.SubscribeAll, args: Array<Any>): Any {
        val topicList = (args[0] as Map<String, QoS>).toList()
        return if (topicList.size == 1) {
            client.subscribe(topicList[0])
        } else {
            client.subscribe(topicList[0], *topicList.toTypedArray().sliceArray(IntRange(1, topicList.size - 1)))
        }
    }

    private fun <T> Message.adapt(topic: String, messageAdapter: MessageAdapter<T>): T? {
        return try {
            val message = messageAdapter.fromMessage(topic, this)
            logger.d("Coordinator", "Message after parsing: $message")
            message
        } catch (th: Throwable) {
            logger.e("Coordinator", "Message parsing exception ${th.message}")
            null
        }
    }
}
