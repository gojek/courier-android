package com.gojek.mqtt.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.gojek.mqtt.persistence.model.MqttReceivePacket

@Dao
internal interface IncomingMessagesDao {
    @Insert
    fun addMessage(mqttPacket: MqttReceivePacket)

    @Query("SELECT * from incoming_messages where topic in (:topics)")
    fun getAllMessagesWithTopicFilter(topics: Set<String>): List<MqttReceivePacket>

    @Query("SELECT * from incoming_messages where topic LIKE :topic")
    fun getAllIncomingMessagesForWildCardTopic(topic: String): List<MqttReceivePacket>

    @Query("DELETE from incoming_messages")
    fun clearAllMessages()

    @Query("DELETE from incoming_messages where msg_id in (:messageIds)")
    fun removeMessagesById(messageIds: List<Long>): Int

    @Query("DELETE from incoming_messages where ts < :timestampNanos")
    fun removeMessagesWithOlderTimestamp(timestampNanos: Long): Int
}
