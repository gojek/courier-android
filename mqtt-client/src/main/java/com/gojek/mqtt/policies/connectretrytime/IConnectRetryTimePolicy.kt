package com.gojek.mqtt.policies.connectretrytime

import com.gojek.mqtt.policies.IFallbackPolicy

interface IConnectRetryTimePolicy : IFallbackPolicy {
    fun getConnRetryTimeSecs(): Int
    fun getConnRetryTimeSecs(forceExp: Boolean): Int
    fun getRetryCount(): Int
    fun getCurrentRetryTime(): Int
}
