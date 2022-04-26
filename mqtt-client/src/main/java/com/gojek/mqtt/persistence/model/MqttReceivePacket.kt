package com.gojek.mqtt.persistence.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "incoming_messages")
internal data class MqttReceivePacket(
    @ColumnInfo(name = "message")
    var message: ByteArray,
    @ColumnInfo(name = "msg_id")
    @PrimaryKey(autoGenerate = true)
    var messageId: Long,
    @ColumnInfo(name = "ts")
    var nanosTimestamp: Long,
    @ColumnInfo(name = "topic")
    var topic: String
)