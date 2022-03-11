package com.gojek.courier

import java.lang.reflect.Type

/**
 * Adapts a [Stream] into another type.
 */
interface StreamAdapter<T, out R> {

    /**
     * Returns an object that delegates to [stream].
     */
    fun adapt(stream: Stream<T>): R

    /**
     * Creates [MessageAdapter] instances based on the return type of [the service interface][Courier.create] methods.
     */
    interface Factory {

        /**
         * Returns a [StreamAdapter] for adapting an [type] into another type, throws an exception if [type] cannot be
         * handled by this factory.
         */
        fun create(type: Type): StreamAdapter<Any, Any>
    }
}
