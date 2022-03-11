package com.gojek.courier.streamadapter.rxjava2

import com.gojek.courier.Stream
import com.gojek.courier.StreamAdapter
import io.reactivex.Observable

internal class ObservableStreamAdapter<T> : StreamAdapter<T, Observable<T>> {

    override fun adapt(stream: Stream<T>): Observable<T> = Observable.fromPublisher(stream)
}
