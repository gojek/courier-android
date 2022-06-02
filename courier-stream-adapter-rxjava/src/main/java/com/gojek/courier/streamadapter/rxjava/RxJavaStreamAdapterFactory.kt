package com.gojek.courier.streamadapter.rxjava

import com.gojek.courier.Stream
import com.gojek.courier.StreamAdapter
import com.gojek.courier.utils.getRawType
import java.lang.reflect.Type
import rx.Observable
import rx.Subscriber

class RxJavaStreamAdapterFactory : StreamAdapter.Factory {

    override fun create(type: Type): StreamAdapter<Any, Any> = when (type.getRawType()) {
        Observable::class.java -> ObservableStreamAdapter()
        else -> throw IllegalArgumentException(
            "$type is not supported by this StreamAdapterFactory"
        )
    }
}

internal class ObservableStreamAdapter<T> : StreamAdapter<T, Observable<T>> {

    override fun adapt(stream: Stream<T>): Observable<T> = Observable.unsafeCreate(
        StreamOnSubscribe(stream)
    )

    private class StreamOnSubscribe<T>(private val stream: Stream<T>) : Observable.OnSubscribe<T> {

        override fun call(subscriber: Subscriber<in T>) {
            stream.start(StreamObserver(subscriber))
        }

        private class StreamObserver<in R>(
            private val subscriber: Subscriber<in R>
        ) : Stream.Observer<R> {
            override fun onNext(data: R) {
                subscriber.onNext(data)
            }

            override fun onError(throwable: Throwable) {
                subscriber.onError(throwable)
            }

            override fun onComplete() {
                subscriber.onCompleted()
            }
        }
    }
}
