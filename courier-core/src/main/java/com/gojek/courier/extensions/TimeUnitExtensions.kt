package com.gojek.courier.extensions

import androidx.annotation.RestrictTo
import java.util.concurrent.TimeUnit

@RestrictTo(RestrictTo.Scope.LIBRARY)
fun Long.fromNanosToMillis(): Long = TimeUnit.NANOSECONDS.toMillis(this)

@RestrictTo(RestrictTo.Scope.LIBRARY)
fun Long.fromMinutesToSeconds() = TimeUnit.MINUTES.toSeconds(this)

@RestrictTo(RestrictTo.Scope.LIBRARY)
fun Long.fromMinutesToMillis() = TimeUnit.MINUTES.toMillis(this)

@RestrictTo(RestrictTo.Scope.LIBRARY)
fun Int.fromMinutesToSeconds() = this.toLong().fromMinutesToSeconds()

@RestrictTo(RestrictTo.Scope.LIBRARY)
fun Int.fromMinutesToMillis() = this.toLong().fromMinutesToMillis()

@RestrictTo(RestrictTo.Scope.LIBRARY)
fun Long.fromMillisToSeconds() = TimeUnit.MILLISECONDS.toSeconds(this)
