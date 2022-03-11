package com.gojek.keepalive.config

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY)
data class AdaptiveKeepAliveConfig(
    val lowerBoundMinutes: Int,
    val upperBoundMinutes: Int,
    val stepMinutes: Int,
    val optimalKeepAliveResetLimit: Int
)
