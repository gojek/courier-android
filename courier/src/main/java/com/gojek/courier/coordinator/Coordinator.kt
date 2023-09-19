package com.gojek.courier.coordinator

import com.gojek.courier.Message
import com.gojek.courier.MessageAdapter
import com.gojek.courier.QoS
import com.gojek.courier.Stream
import com.gojek.courier.Stream.Disposable
import com.gojek.courier.Stream.Observer
import com.gojek.courier.logging.ILogger
import com.gojek.courier.stub.StubInterface
import com.gojek.courier.stub.StubMethod
import com.gojek.courier.utils.toStream
import com.gojek.mqtt.client.MqttClient
import com.gojek.mqtt.client.listener.MessageListener
import com.gojek.mqtt.client.model.ConnectionState
import com.gojek.mqtt.client.model.MqttMessage
import com.gojek.mqtt.event.EventHandler
import com.gojek.mqtt.event.MqttEvent
import com.gojek.mqtt.event.MqttEvent.MqttSubscribeFailureEvent
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.FlowableOnSubscribe
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import org.eclipse.paho.client.mqttv3.MqttException
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription

internal class Coordinator(
    private val client: MqttClient,
    private val logger: ILogger
) : StubInterface.Callback {

    private val eventSubject = PublishSubject.create<MqttEvent> { emitter ->
        val eventHandler = object : EventHandler {
            override fun onEvent(mqttEvent: MqttEvent) {
                if (emitter.isDisposed.not()) {
                    emitter.onNext(mqttEvent)
                }
            }
        }
        client.addEventHandler(eventHandler)
        emitter.setCancellable { client.removeEventHandler(eventHandler) }
    }

    @Synchronized
    override fun send(stubMethod: StubMethod.Send, args: Array<Any>): Any {
        logger.d("Coordinator", "Send method invoked")
        val data = stubMethod.argumentProcessor.getDataArgument(args)
        stubMethod.argumentProcessor.inject(args)
        val topic = stubMethod.argumentProcessor.getTopic()
        val callback = stubMethod.argumentProcessor.getCallbackArgument(args)
        val message = stubMethod.messageAdapter.toMessage(topic, data)
        val qos = stubMethod.qos
        val sent = client.send(message, topic, qos, callback)
        logger.d("Coordinator", "Sending message on topic: $topic, qos: $qos, message: $data")
        return sent
    }

    @Synchronized
    override fun receive(stubMethod: StubMethod.Receive, args: Array<Any>): Any {
        logger.d("Coordinator", "Receive method invoked")
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
        logger.d("Coordinator", "Subscribe method invoked")
        stubMethod.argumentProcessor.inject(args)
        val topic = stubMethod.argumentProcessor.getTopic()
        val qos = stubMethod.qos
        val status = client.subscribe(topic to qos)
        logger.d("Coordinator", "Subscribing topic: $topic with qos: $qos")
        return status
    }

    override fun subscribeWithStream(
        stubMethod: StubMethod.SubscribeWithStream,
        args: Array<Any>
    ): Any {
        logger.d("Coordinator", "Subscribe method invoked with a returning stream")
        stubMethod.argumentProcessor.inject(args)
        val topic = stubMethod.argumentProcessor.getTopic()
        val qos = stubMethod.qos
        client.subscribe(topic to qos)
        logger.d("Coordinator", "Subscribing topic: $topic with qos: $qos")

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
                val eventDisposable = eventSubject.filter { event ->
                    isInvalidSubscriptionFailureEvent(event, topic)
                }.subscribe {
                    client.removeMessageListener(topic, listener)
                }
                emitter.setCancellable {
                    client.removeMessageListener(topic, listener)
                    eventDisposable.dispose()
                }
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
        logger.d("Coordinator", "Unsubscribe method invoked")
        stubMethod.argumentProcessor.inject(args)
        val topics = stubMethod.argumentProcessor.getTopics()
        val status = if (topics.size == 1) {
            client.unsubscribe(topics[0])
        } else {
            client.unsubscribe(topics[0], *topics.sliceArray(IntRange(1, topics.size - 1)))
        }
        logger.d("Coordinator", "Unsubscribing topics: $topics")
        return status
    }

    override fun subscribeAll(stubMethod: StubMethod.SubscribeAll, args: Array<Any>): Any {
        logger.d("Coordinator", "Subscribe method invoked for multiple topics")
        val topicList = (args[0] as Map<String, QoS>).toList()
        val status = if (topicList.size == 1) {
            client.subscribe(topicList[0])
        } else {
            client.subscribe(topicList[0], *topicList.toTypedArray().sliceArray(IntRange(1, topicList.size - 1)))
        }
        logger.d("Coordinator", "Subscribing topics: $topicList")
        return status
    }

    override fun subscribeAllWithStream(stubMethod: StubMethod.SubscribeAllWithStream, args: Array<Any>): Any {
        logger.d("Coordinator", "Subscribe method invoked for multiple topics")
        val topicList = (args[0] as Map<String, QoS>).toList()
        if (topicList.size == 1) {
            client.subscribe(topicList[0])
        } else {
            client.subscribe(topicList[0], *topicList.toTypedArray().sliceArray(IntRange(1, topicList.size - 1)))
        }
        logger.d("Coordinator", "Subscribed topics: $topicList")
        val flowable = Flowable.create(
            FlowableOnSubscribe<MqttMessage> { emitter ->
                val listener = object : MessageListener {
                    override fun onMessageReceived(mqttMessage: MqttMessage) {
                        if (emitter.isCancelled.not()) {
                            emitter.onNext(mqttMessage)
                        }
                    }
                }
                val eventDisposable = CompositeDisposable()
                for (topic in topicList) {
                    client.addMessageListener(topic.first, listener)
                    eventDisposable.add(
                        eventSubject.filter { event ->
                            isInvalidSubscriptionFailureEvent(event, topic.first)
                        }.subscribe {
                            client.removeMessageListener(topic.first, listener)
                        }
                    )
                }
                emitter.setCancellable {
                    for (topic in topicList) {
                        client.removeMessageListener(topic.first, listener)
                        eventDisposable.dispose()
                    }
                }
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

    override fun getEventStream(): Stream<MqttEvent> {
        return object : Stream<MqttEvent> {
            override fun start(observer: Observer<MqttEvent>): Disposable {
                val eventHandler = object : EventHandler {
                    override fun onEvent(mqttEvent: MqttEvent) {
                        try {
                            observer.onNext(mqttEvent)
                        } catch (throwable: Throwable) {
                            observer.onError(throwable)
                        }
                    }
                }
                client.addEventHandler(eventHandler)
                var isDisposed = false
                return object : Disposable {
                    override fun dispose() {
                        client.removeEventHandler(eventHandler)
                        isDisposed = true
                    }

                    override fun isDisposed(): Boolean {
                        return isDisposed
                    }
                }
            }

            override fun subscribe(s: Subscriber<in MqttEvent>) {
                val eventHandler = object : EventHandler {
                    override fun onEvent(mqttEvent: MqttEvent) {
                        try {
                            s.onNext(mqttEvent)
                        } catch (throwable: Throwable) {
                            s.onError(throwable)
                        }
                    }
                }
                s.onSubscribe(object : Subscription {
                    override fun request(n: Long) {
                        client.addEventHandler(eventHandler)
                    }

                    override fun cancel() {
                        client.removeEventHandler(eventHandler)
                    }
                })
            }
        }
    }

    override fun getConnectionState(): ConnectionState {
        return client.getCurrentState()
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

    private fun isInvalidSubscriptionFailureEvent(event: MqttEvent, topic: String): Boolean {
        return event is MqttSubscribeFailureEvent &&
            event.topics.containsKey(topic) &&
            event.exception.reasonCode == MqttException.REASON_CODE_INVALID_SUBSCRIPTION.toInt()
    }
}
