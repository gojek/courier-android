package com.gojek.courier

import java.lang.reflect.Type

interface MessageAdapter<T> {

    /** Returns an object of type `T` that represents a [Message]. */
    fun fromMessage(message: Message): T

    /** Returns a [Message] that represents [data]. */
    fun toMessage(data: T): Message

    /** Creates [MessageAdapter] instances based on a type and target usage. */
    interface Factory {

        /**
         * Returns a [MessageAdapter] for adapting an [type] from and to [Message], throws an exception if [type] cannot
         * be handled by this factory.
         */
        fun create(type: Type, annotations: Array<Annotation>): MessageAdapter<*>
    }
}
