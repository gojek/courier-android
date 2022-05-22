package com.gojek.mqtt.policies.connecttimeout

import com.gojek.mqtt.policies.IFallbackPolicy

interface IConnectTimeoutPolicy : IFallbackPolicy {
    fun getConnectTimeOut(): Int
    fun getHandshakeTimeOut(): Int
    fun updateParams(isSslPort: Boolean)
}
