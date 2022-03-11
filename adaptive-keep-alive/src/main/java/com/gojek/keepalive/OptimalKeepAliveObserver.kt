package com.gojek.keepalive

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY)
interface OptimalKeepAliveObserver {
    fun onOptimalKeepAliveFound(
        timeMinutes: Int,
        probeCount: Int,
        convergenceTime: Int
    )

    fun onOptimalKeepAliveNotFound(
        timeMinutes: Int,
        probeCount: Int,
        convergenceTime: Int
    )
}