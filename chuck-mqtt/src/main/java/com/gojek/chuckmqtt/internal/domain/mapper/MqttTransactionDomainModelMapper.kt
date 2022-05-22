package com.gojek.chuckmqtt.internal.domain.mapper

import com.gojek.chuckmqtt.internal.base.Mapper
import com.gojek.chuckmqtt.internal.data.local.entity.MqttTransaction
import com.gojek.chuckmqtt.internal.domain.model.MqttTransactionDomainModel
import org.eclipse.paho.client.mqttv3.internal.wire.MqttWireMessage

internal class MqttTransactionDomainModelMapper : Mapper<MqttTransaction, MqttTransactionDomainModel> {

    override fun map(input: MqttTransaction): MqttTransactionDomainModel {
        return with(input) {
            MqttTransactionDomainModel(
                id = id,
                mqttWireMessage = mqttWireMessageFromBytes(mqttWireMessageBytes!!),
                isSent = isPublished,
                transmissionTime = requestDate,
                sizeInBytes = sizeInBytes
            )
        }
    }

    private fun mqttWireMessageFromBytes(mqttWireMessageBytes: ByteArray): MqttWireMessage {
        return MqttWireMessage.createWireMessage(mqttWireMessageBytes, null)
    }
}
