package com.gojek.mqtt.client.event.interceptor

import com.gojek.mqtt.event.EventHandler
import com.gojek.mqtt.event.MqttEvent
import java.util.concurrent.CopyOnWriteArrayList

internal class MqttEventHandler : EventHandler {

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
        eventHandlers.forEach { it.onEvent(mqttEvent) }
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
