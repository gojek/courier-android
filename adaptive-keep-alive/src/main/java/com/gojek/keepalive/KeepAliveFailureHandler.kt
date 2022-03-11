package com.gojek.keepalive

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY)
interface KeepAliveFailureHandler {
    fun handleKeepAliveFailure()
}

@RestrictTo(RestrictTo.Scope.LIBRARY)
class NoOpKeepAliveFailureHandler : KeepAliveFailureHandler {
    override fun handleKeepAliveFailure() = Unit
}