package com.gojek.courier.messageadapter.builtin

import com.gojek.courier.Message
import com.gojek.courier.MessageAdapter

internal class TextMessageAdapter : MessageAdapter<String> {

    override fun fromMessage(message: Message): String = when (message) {
        is Message.Bytes -> String(message.value)
    }

    override fun toMessage(data: String): Message = Message.Bytes(data.toByteArray())
}
