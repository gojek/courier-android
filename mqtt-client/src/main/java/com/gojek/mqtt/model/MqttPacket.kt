package com.gojek.mqtt.model

import com.gojek.courier.QoS

internal data class MqttPacket(
    val message: ByteArray,
    val topic: String,
    val qos: QoS
)
