package com.gojek.mqtt.persistence

import com.gojek.mqtt.persistence.model.MqttReceivePacket

internal interface IMqttReceivePersistence {
    fun addReceivedMessage(mqttPacket: MqttReceivePacket)
    fun getAllIncomingMessagesWithTopicFilter(topics: Set<String>): List<MqttReceivePacket>
    fun removeReceivedMessages(messageIds: List<Long>): Int
    fun removeMessagesWithOlderTimestamp(timestampNanos: Long): Int
    fun getAllIncomingMessagesForWildCardTopic(topic: String): List<MqttReceivePacket>
}
