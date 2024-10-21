package com.gojek.mqtt.model

import com.gojek.courier.QoS

data class Will(
    val topic: String,
    val message: String,
    val qos: QoS,
    val retained: Boolean
)
