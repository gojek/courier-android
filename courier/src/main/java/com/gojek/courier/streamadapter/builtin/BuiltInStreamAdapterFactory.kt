package com.gojek.courier.streamadapter.builtin

import com.gojek.courier.Stream
import com.gojek.courier.StreamAdapter
import com.gojek.courier.utils.getRawType
import java.lang.reflect.Type

internal class BuiltInStreamAdapterFactory : StreamAdapter.Factory {

    override fun create(type: Type): StreamAdapter<Any, Any> = when (type.getRawType()) {
        Stream::class.java -> IdentityStreamAdapter()
        else -> throw IllegalArgumentException("$type is not supported.")
    }
}
