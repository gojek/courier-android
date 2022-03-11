package com.gojek.courier.messageadapter.builtin

import com.gojek.courier.MessageAdapter
import com.gojek.courier.utils.getRawType
import java.lang.reflect.Type

internal class BuiltInMessageAdapterFactory : MessageAdapter.Factory {

    override fun create(type: Type, annotations: Array<Annotation>): MessageAdapter<*> = when (type.getRawType()) {
        String::class.java -> TextMessageAdapter()
        ByteArray::class.java -> ByteArrayMessageAdapter()
        else -> throw IllegalArgumentException("Type is not supported by this MessageAdapterFactory: $type")
    }
}
