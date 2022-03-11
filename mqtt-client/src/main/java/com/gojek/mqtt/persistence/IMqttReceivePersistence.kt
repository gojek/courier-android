package com.gojek.mqtt.persistence

import com.gojek.mqtt.persistence.model.MqttReceivePacket

internal interface IMqttReceivePersistence {
    fun addReceivedMessage(mqttPacket: MqttReceivePacket)
    fun getAllIncomingMessages(): List<MqttReceivePacket>
    fun removeReceivedMessage(mqttPacket: MqttReceivePacket)
}