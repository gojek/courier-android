@file:JvmName("FlowableUtils")

package com.gojek.courier.utils

import io.reactivex.Flowable

internal fun <T> Flowable<T>.toStream() = FlowableStream(this)
