package com.gojek.mqtt.client.event.interceptor

import com.gojek.mqtt.event.EventHandler
import com.gojek.mqtt.event.MqttEvent
import com.gojek.mqtt.utils.MqttUtils
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit.SECONDS

internal class MqttEventHandler(
    mqttUtils: MqttUtils
) : EventHandler {

    private val eventScheduler = ThreadPoolExecutor(
        /* corePoolSize = */ 1,
        /* maximumPoolSize = */ 1,
        /* keepAliveTime = */ 300,
        /* unit = */ SECONDS,
        /* workQueue = */ LinkedBlockingQueue(),
        /* threadFactory = */ mqttUtils.threadFactory("mqtt-event-handler", false)
    ).apply { allowCoreThreadTimeOut(true) }
    private val interceptorList = CopyOnWriteArrayList<EventInterceptor>()
    private val eventHandlers = CopyOnWriteArrayList<EventHandler>()

    init {
        interceptorList.add(ConnectionInfoInterceptor())
    }

    override fun onEvent(mqttEvent: MqttEvent) {
        var event = mqttEvent
        interceptorList.forEach {
            event = it.intercept(event)
        }
        if (eventHandlers.isNotEmpty()) {
            eventScheduler.submit {
                eventHandlers.forEach { it.onEvent(mqttEvent) }
            }
        }
    }

    fun addEventHandler(handler: EventHandler) {
        eventHandlers.addIfAbsent(handler)
    }

    fun removeEventHandler(handler: EventHandler) {
        eventHandlers.remove(handler)
    }

    fun addInterceptor(interceptor: EventInterceptor) {
        interceptorList.addIfAbsent(interceptor)
    }
}
