package com.gojek.courier.utils

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY)
class Clock {
    fun currentTimeMillis(): Long {
        return System.currentTimeMillis()
    }

    fun nanoTime(): Long {
        return System.nanoTime()
    }
}
