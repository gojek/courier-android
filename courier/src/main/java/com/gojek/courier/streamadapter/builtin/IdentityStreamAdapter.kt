package com.gojek.courier.streamadapter.builtin

import com.gojek.courier.Stream
import com.gojek.courier.StreamAdapter

internal class IdentityStreamAdapter<T> : StreamAdapter<T, Stream<T>> {

    override fun adapt(stream: Stream<T>): Stream<T> = stream
}
