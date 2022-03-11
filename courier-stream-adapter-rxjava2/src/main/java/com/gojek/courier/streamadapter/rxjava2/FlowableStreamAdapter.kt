package com.gojek.courier.streamadapter.rxjava2

import com.gojek.courier.Stream
import com.gojek.courier.StreamAdapter
import io.reactivex.Flowable

internal class FlowableStreamAdapter<T> : StreamAdapter<T, Flowable<T>> {

    override fun adapt(stream: Stream<T>): Flowable<T> = Flowable.fromPublisher(stream)
}
