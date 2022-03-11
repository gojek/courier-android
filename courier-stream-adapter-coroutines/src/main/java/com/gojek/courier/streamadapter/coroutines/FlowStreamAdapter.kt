package com.gojek.courier.streamadapter.coroutines

import com.gojek.courier.Stream
import com.gojek.courier.StreamAdapter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow

internal class FlowStreamAdapter<T> : StreamAdapter<T, Flow<T>> where T : Any {
    override fun adapt(stream: Stream<T>) = stream.asFlow()
}