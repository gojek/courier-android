package com.gojek.mqtt.send.listener

import com.gojek.mqtt.client.model.MqttSendPacket

internal interface IMessageSendListener {
    fun onSuccess(packet: MqttSendPacket)
    fun onFailure(packet: MqttSendPacket, exception: Throwable)
    fun notifyWrittenOnSocket(packet: MqttSendPacket)
}
