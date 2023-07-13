package com.gojek.mqtt.client.model

import android.os.Parcel
import android.os.Parcelable
import com.gojek.courier.callback.NoOpSendMessageCallback
import com.gojek.courier.callback.SendMessageCallback

internal data class MqttSendPacket(
    var message: ByteArray,
    var messageId: Long,
    var timestamp: Long,
    var qos: Int,
    var topic: String,
    var type: Int,
    var triggerTime: Long,
    var sendMessageCallback: SendMessageCallback
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.createByteArray()!!,
        parcel.readLong(),
        parcel.readLong(),
        parcel.readInt(),
        parcel.readString()!!,
        parcel.readInt(),
        parcel.readLong(),
        NoOpSendMessageCallback
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeByteArray(message)
        parcel.writeLong(messageId)
        parcel.writeLong(timestamp)
        parcel.writeInt(qos)
        parcel.writeString(topic)
        parcel.writeInt(type)
        parcel.writeLong(triggerTime)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<MqttSendPacket> {
        override fun createFromParcel(parcel: Parcel): MqttSendPacket {
            return MqttSendPacket(parcel)
        }

        override fun newArray(size: Int): Array<MqttSendPacket?> {
            return arrayOfNulls(size)
        }
    }
}
