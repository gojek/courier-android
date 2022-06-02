package com.gojek.mqtt.client.model

import com.gojek.courier.Message

data class MqttMessage(
    val topic: String,
    val message: Message
)
