package com.gojek.courier

sealed class Message {
    /**
     * Represents a binary message.
     *
     * @property value The binary data.
     */
    class Bytes(val value: ByteArray) : Message() {
        operator fun component1(): ByteArray = value
    }
}
