package com.gojek.mqtt.send.listener

import com.gojek.mqtt.client.model.MqttSendPacket

internal class NoOpMessageSendListener :
    IMessageSendListener {
    override fun onSuccess(packet: MqttSendPacket) {
    }

    override fun onFailure(packet: MqttSendPacket, exception: Throwable) {
    }

    override fun notifyWrittenOnSocket(packet: MqttSendPacket) {
    }
}
