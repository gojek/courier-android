package com.gojek.mqtt.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gojek.mqtt.persistence.model.MqttPahoPacket

@Dao
internal interface PahoMessagesDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMessage(mqttPacket: MqttPahoPacket)

    @Query("DELETE from paho_messages where `key`=:key")
    fun deleteMessageByKey(key: String): Int

    @Query("DELETE from paho_messages")
    fun clearAllMessages(): Int

    @Query("SELECT * from paho_messages where `key`=:key")
    fun getMessageByKey(key: String): MqttPahoPacket

    @Query("SELECT count(`key`) from paho_messages where `key`=:key")
    fun containsMessage(key: String): Int

    @Query("SELECT `key` from paho_messages")
    fun getAllKeys(): List<String>

    @Query("SELECT * from paho_messages")
    fun getAllMessages(): List<MqttPahoPacket>
}
