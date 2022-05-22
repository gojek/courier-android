package com.gojek.keepalive

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY)
class OptimalKeepAliveFailureHandler(
    private val optimalKeepAliveProvider: OptimalKeepAliveProvider
) : KeepAliveFailureHandler {
    override fun handleKeepAliveFailure() {
        optimalKeepAliveProvider.onOptimalKeepAliveFailure()
    }
}
