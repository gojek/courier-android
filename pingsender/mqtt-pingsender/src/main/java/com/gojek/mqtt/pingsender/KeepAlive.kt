package com.gojek.mqtt.pingsender

import androidx.annotation.RestrictTo
import com.gojek.courier.extensions.fromMinutesToMillis

@RestrictTo(RestrictTo.Scope.LIBRARY)
data class KeepAlive(
    val networkType: Int,
    val networkName: String,
    val keepAliveMinutes: Int
)

@RestrictTo(RestrictTo.Scope.LIBRARY)
fun KeepAlive.keepAliveMillis() = keepAliveMinutes.fromMinutesToMillis()
