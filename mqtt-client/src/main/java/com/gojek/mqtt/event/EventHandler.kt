package com.gojek.mqtt.event

interface EventHandler {
    fun onEvent(mqttEvent: MqttEvent) = Unit
}