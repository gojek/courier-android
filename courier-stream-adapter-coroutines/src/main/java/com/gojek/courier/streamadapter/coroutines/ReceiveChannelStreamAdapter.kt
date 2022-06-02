package com.gojek.courier.streamadapter.coroutines

import com.gojek.courier.Stream
import com.gojek.courier.StreamAdapter
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.reactive.openSubscription

internal class ReceiveChannelStreamAdapter<T> : StreamAdapter<T, ReceiveChannel<T>> {

    override fun adapt(stream: Stream<T>) = stream.openSubscription()
}
