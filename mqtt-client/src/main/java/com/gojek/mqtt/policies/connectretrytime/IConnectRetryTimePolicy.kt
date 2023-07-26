package com.gojek.mqtt.policies.connectretrytime

import com.gojek.mqtt.policies.IFallbackPolicy

interface IConnectRetryTimePolicy : IFallbackPolicy {
    fun getConnRetryTimeSecs(isAuthFailure: Boolean = false): Int
}
