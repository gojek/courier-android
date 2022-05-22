package com.gojek.mqtt.client.event.interceptor

import com.gojek.mqtt.event.MqttEvent

internal interface EventInterceptor {
    fun intercept(mqttEvent: MqttEvent): MqttEvent
}
