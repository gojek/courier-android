package com.gojek.mqtt.persistence.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "paho_messages")
internal data class MqttPahoPacket(
    @ColumnInfo(name = "key")
    @PrimaryKey
    var key: String,
    @ColumnInfo(name = "header_bytes")
    var headerBytes: ByteArray,
    @ColumnInfo(name = "header_offset")
    var headerOffset: Int,
    @ColumnInfo(name = "header_length")
    var headerLength: Int,
    @ColumnInfo(name = "payload_bytes")
    var payloadBytes: ByteArray,
    @ColumnInfo(name = "payload_offset")
    var payloadOffset: Int,
    @ColumnInfo(name = "payload_length")
    var payloadLength: Int
)