package com.gojek.mqtt.client.listener

import com.gojek.mqtt.client.model.MqttMessage

interface MessageListener {
    fun onMessageReceived(mqttMessage: MqttMessage)
}
