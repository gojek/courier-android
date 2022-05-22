package com.gojek.courier.streamadapter.coroutines

import com.gojek.courier.StreamAdapter
import com.gojek.courier.utils.getRawType
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.Flow
import java.lang.reflect.Type

/**
 * A [stream adapter factory][StreamAdapter.Factory] that uses ReceiveChannel.
 */
class CoroutinesStreamAdapterFactory : StreamAdapter.Factory {

    override fun create(type: Type): StreamAdapter<Any, Any> {
        return when (type.getRawType()) {
            ReceiveChannel::class.java -> ReceiveChannelStreamAdapter()
            Flow::class.java -> FlowStreamAdapter()
            else -> throw IllegalArgumentException()
        }
    }
}
