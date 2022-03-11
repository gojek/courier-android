package com.gojek.mqtt.client

import com.gojek.courier.QoS
import com.gojek.courier.logging.ILogger
import com.gojek.mqtt.event.EventHandler
import com.gojek.mqtt.event.MqttEvent.MqttMessageReceiveErrorEvent
import com.gojek.mqtt.exception.toCourierException
import com.gojek.mqtt.model.MqttPacket
import com.gojek.mqtt.persistence.IMqttReceivePersistence
import com.gojek.mqtt.persistence.model.MqttReceivePacket
import com.gojek.mqtt.utils.MqttUtils
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

internal class IncomingMsgControllerImpl(
    private val mqttUtils: MqttUtils,
    private val publishSubject: PublishSubject<MqttPacket>,
    private val mqttReceivePersistence: IMqttReceivePersistence,
    private val logger: ILogger,
    private val eventHandler: EventHandler
): IncomingMsgController {
    private val threadPool: ThreadPoolExecutor
    private val trigger: HandleMessage

    init {
        threadPool = ThreadPoolExecutor(
            1,
            1,
            60,
            TimeUnit.SECONDS,
            LinkedBlockingQueue<Runnable>(1),
            mqttUtils.threadFactory("msg-store", false)
        )
        threadPool.rejectedExecutionHandler = ThreadPoolExecutor.DiscardPolicy()
        trigger = HandleMessage()
    }

    override fun triggerHandleMessage() {
        threadPool.submit(trigger)
    }

    private inner class HandleMessage : Runnable {
        override fun run() {
            // get Messages from DB and handle here
            val messages: List<MqttReceivePacket> =
                mqttReceivePersistence.getAllIncomingMessages()
            if (mqttUtils.isEmpty(messages)) {
                logger.d(TAG, "No Messages in Table")
                return
            }
            for (message in messages) {
                logger.d(TAG, "Going to process ${message.messageId}")
                if (!sendMessage(message)) {
                    deleteMessage(message)
                    continue
                }
                logger.d(TAG, "Successfully Processed Message ${message.messageId}")
                deleteMessage(message)
            }
        }

        private fun deleteMessage(message: MqttReceivePacket) {
            mqttReceivePersistence.removeReceivedMessage(message)
        }
    }

    fun sendMessage(message: MqttReceivePacket): Boolean {
        try {
            publishSubject.onNext(MqttPacket(message.message, message.topic, QoS.ONE))
            return true
        } catch (e: Throwable) {
            // catching exception here and removing the message from Db
            logger.d(TAG, "Exception while prcessing message $e")
            eventHandler.onEvent(MqttMessageReceiveErrorEvent(message.topic, message.message.size, e.toCourierException()))
        }
        return false
    }

    companion object {
        const val TAG = "IncomingMsgController"
    }
}