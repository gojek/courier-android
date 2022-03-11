package com.gojek.courier.messageadapter.builtin

import com.gojek.courier.Message
import com.gojek.courier.MessageAdapter

internal class ByteArrayMessageAdapter : MessageAdapter<ByteArray> {

    override fun fromMessage(message: Message): ByteArray = when (message) {
        is Message.Bytes -> message.value
    }

    override fun toMessage(data: ByteArray): Message = Message.Bytes(data)
}
