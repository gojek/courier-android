package com.gojek.chuckmqtt.internal.utils.extensions

internal fun ifTrue(p1: Boolean?, block: () -> Unit) {
    if (p1 == true) {
        block()
    }
}