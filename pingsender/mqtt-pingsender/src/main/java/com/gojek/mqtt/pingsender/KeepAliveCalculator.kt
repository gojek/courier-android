package com.gojek.mqtt.pingsender

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY)
interface KeepAliveCalculator {
    fun getUnderTrialKeepAlive(): KeepAlive
    fun onKeepAliveSuccess(keepAlive: KeepAlive)
    fun onKeepAliveFailure(keepAlive: KeepAlive)
    fun getOptimalKeepAlive(): Int
    fun onOptimalKeepAliveFailure()
}