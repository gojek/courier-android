package com.gojek.chuckmqtt.internal.data.network.model

internal data class CollectorModel(
    val isSent: Boolean,
    val messageBytes: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CollectorModel

        if (isSent != other.isSent) return false
        if (!messageBytes.contentEquals(other.messageBytes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = isSent.hashCode()
        result = 31 * result + messageBytes.contentHashCode()
        return result
    }
}