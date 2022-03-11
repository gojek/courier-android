package com.gojek.mqtt.persistence.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.gojek.mqtt.persistence.model.MqttReceivePacket

@Dao
internal interface IncomingMessagesDao {
    @Insert
    fun addMessage(mqttPacket: MqttReceivePacket)

    @Delete
    fun removeMessage(mqttPacket: MqttReceivePacket)

    @Query("SELECT * from incoming_messages")
    fun getAllMessages(): List<MqttReceivePacket>

    @Query("DELETE from incoming_messages")
    fun clearAllMessages()
}