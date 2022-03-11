package com.gojek.mqtt.client.event.interceptor

import com.gojek.mqtt.event.EventHandler
import com.gojek.mqtt.event.MqttEvent
import java.util.LinkedList
import java.util.concurrent.CopyOnWriteArrayList

internal class MqttEventsInterceptor(private val eventHandler: EventHandler) : EventHandler {

    private val interceptorList = CopyOnWriteArrayList<EventInterceptor>()

    init {
        interceptorList.add(ConnectionInfoInterceptor())
    }

    override fun onEvent(mqttEvent: MqttEvent) {
        var event = mqttEvent
        interceptorList.forEach {
            event = it.intercept(event)
        }
        eventHandler.onEvent(event)
    }

    fun addInterceptor(interceptor: EventInterceptor) {
        interceptorList.add(interceptor)
    }
}
