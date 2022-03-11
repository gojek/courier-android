package com.gojek.chuckmqtt.internal.base

internal interface Mapper<in I, out O> {
    fun map(input: I): O
}