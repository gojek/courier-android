package com.gojek.chuckmqtt.internal.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
internal data class MqttTransaction(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") var id: Long = 0,
    @ColumnInfo(name = "mqtt_wire_message_bytes") val mqttWireMessageBytes: ByteArray?,
    @ColumnInfo(name = "is_published") var isPublished: Boolean,
    @ColumnInfo(name = "transmission_time") var requestDate: Long,
    @ColumnInfo(name = "size_in_bytes") var sizeInBytes: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MqttTransaction

        if (id != other.id) return false
        if (mqttWireMessageBytes != null) {
            if (other.mqttWireMessageBytes == null) return false
            if (mqttWireMessageBytes.contentEquals(other.mqttWireMessageBytes)) return false
        } else if (other.mqttWireMessageBytes != null) return false
        if (isPublished != other.isPublished) return false
        if (requestDate != other.requestDate) return false
        if (sizeInBytes != other.sizeInBytes) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + (mqttWireMessageBytes?.contentHashCode() ?: 0)
        result = 31 * result + isPublished.hashCode()
        result = 31 * result + requestDate.hashCode()
        result = 31 * result + sizeInBytes.hashCode()
        return result
    }
}
