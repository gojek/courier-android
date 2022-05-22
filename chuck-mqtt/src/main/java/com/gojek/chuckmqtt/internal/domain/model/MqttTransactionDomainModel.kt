package com.gojek.chuckmqtt.internal.domain.model

import org.eclipse.paho.client.mqttv3.internal.wire.MqttWireMessage

internal data class MqttTransactionDomainModel(
    val id: Long,
    val mqttWireMessage: MqttWireMessage,
    val isSent: Boolean,
    val transmissionTime: Long,
    val sizeInBytes: Long
)
