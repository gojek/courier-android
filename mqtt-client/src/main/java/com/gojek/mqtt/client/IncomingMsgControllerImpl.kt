package com.gojek.mqtt.client

import com.gojek.courier.extensions.fromSecondsToNanos
import com.gojek.courier.logging.ILogger
import com.gojek.courier.utils.Clock
import com.gojek.mqtt.client.listener.MessageListener
import com.gojek.mqtt.event.EventHandler
import com.gojek.mqtt.event.MqttEvent.MqttMessageReceiveErrorEvent
import com.gojek.mqtt.exception.toCourierException
import com.gojek.mqtt.persistence.IMqttReceivePersistence
import com.gojek.mqtt.persistence.model.MqttReceivePacket
import com.gojek.mqtt.persistence.model.toMqttMessage
import com.gojek.mqtt.utils.MqttUtils
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

internal class IncomingMsgControllerImpl(
    private val mqttUtils: MqttUtils,
    private val mqttReceivePersistence: IMqttReceivePersistence,
    private val logger: ILogger,
    private val eventHandler: EventHandler,
    private val ttlSeconds: Long,
    private val cleanupIntervalSeconds: Long,
    private val clock: Clock
): IncomingMsgController {
    private val handleMsgThreadPool = ThreadPoolExecutor(
        1,
        1,
        60,
        TimeUnit.SECONDS,
        LinkedBlockingQueue(1),
        mqttUtils.threadFactory("msg-store", false)
    ).apply { rejectedExecutionHandler = ThreadPoolExecutor.DiscardPolicy() }

    private val cleanupThreadPool = ScheduledThreadPoolExecutor(
        1,
        mqttUtils.threadFactory("msg-store-cleanup", false),
        ThreadPoolExecutor.DiscardPolicy()
    )

    private val handleMessageTrigger = HandleMessage()
    private val cleanupMessagesTrigger = CleanupExpiredMessages()

    private val listenerMap = ConcurrentHashMap<String, List<MessageListener>>()

    private var cleanupFuture: ScheduledFuture<*>? = null

    override fun triggerHandleMessage() {
        handleMsgThreadPool.submit(handleMessageTrigger)
    }

    private fun scheduleMessagesCleanup() {
        cleanupFuture?.cancel(false)
        cleanupFuture = cleanupThreadPool.schedule(
            cleanupMessagesTrigger,
            cleanupIntervalSeconds,
            TimeUnit.SECONDS
        )
    }

    @Synchronized
    override fun registerListener(topic: String, listener: MessageListener) {
        listenerMap[topic] = (listenerMap[topic] ?: emptyList()) + listener
        triggerHandleMessage()
    }

    @Synchronized
    override fun unregisterListener(topic: String, listener: MessageListener) {
        listenerMap[topic] = (listenerMap[topic] ?: emptyList()) - listener
        if (listenerMap[topic]!!.isEmpty()) {
            listenerMap.remove(topic)
        }
    }

    private inner class HandleMessage : Runnable {
        override fun run() {
            try {
                if (listenerMap.keys.isEmpty()) {
                    logger.d(TAG, "No listeners registered")
                    return
                }
                val messages: List<MqttReceivePacket> =
                    mqttReceivePersistence.getAllIncomingMessagesWithTopicFilter(listenerMap.keys)
                if (mqttUtils.isEmpty(messages)) {
                    logger.d(TAG, "No Messages in Table")
                    return
                }
                val deletedMsgIds = mutableListOf<Long>()
                for (message in messages) {
                    logger.d(TAG, "Going to process ${message.messageId}")
                    val listenersNotified = notifyListeners(message)
                    if (listenersNotified) {
                        deletedMsgIds.add(message.messageId)
                    }
                    logger.d(TAG, "Successfully Processed Message ${message.messageId}")
                }
                if (deletedMsgIds.isNotEmpty()) {
                    val deletedMessagesCount = deleteMessages(deletedMsgIds)
                    logger.d(TAG, "Deleted $deletedMessagesCount messages")
                }
            } finally {
                scheduleMessagesCleanup()
            }
        }

        private fun deleteMessages(messageIds: List<Long>): Int {
            return mqttReceivePersistence.removeReceivedMessages(messageIds)
        }
    }

    private inner class CleanupExpiredMessages : Runnable {
        override fun run() {
            logger.d(TAG, "Deleting expired messages")
            val currentTime = clock.nanoTime()
            val expiryTime = currentTime - ttlSeconds.fromSecondsToNanos()
            val deletedMsgsCount =
                mqttReceivePersistence.removeMessagesWithOlderTimestamp(expiryTime)
            logger.d(TAG, "Deleted $deletedMsgsCount expired messages")
        }
    }

    private fun notifyListeners(message: MqttReceivePacket): Boolean {
        var notified = false
        try {
            listenerMap[message.topic]!!.forEach {
                notified = true
                it.onMessageReceived(message.toMqttMessage())
            }
            return notified
        } catch (e: Throwable) {
            logger.d(TAG, "Exception while processing message $e")
            eventHandler.onEvent(MqttMessageReceiveErrorEvent(message.topic, message.message.size, e.toCourierException()))
        }
        return notified
    }

    companion object {
        const val TAG = "IncomingMsgController"
    }
}