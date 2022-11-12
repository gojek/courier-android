package com.gojek.courier.messageadapter.text

import com.gojek.courier.Message
import com.gojek.courier.MessageAdapter
import com.gojek.courier.utils.getRawType
import java.lang.reflect.Type

class TextMessageAdapterFactory : MessageAdapter.Factory {
    override fun create(type: Type, annotations: Array<Annotation>): MessageAdapter<*> = when (type.getRawType()) {
        String::class.java -> TextMessageAdapter()
        else -> throw IllegalArgumentException("Type is not supported by this MessageAdapterFactory: $type")
    }
}

internal class TextMessageAdapter : MessageAdapter<String> {
    override fun fromMessage(topic: String, message: Message): String = when (message) {
        is Message.Bytes -> String(message.value)
    }

    override fun toMessage(topic: String, data: String): Message = Message.Bytes(data.toByteArray())

    override fun contentType() = "text/plain"
}
