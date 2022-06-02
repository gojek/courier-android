package com.gojek.mqtt.policies.connecttimeout

class ConnectTimeoutPolicy(
    private val connectTimeoutConfig: ConnectTimeoutConfig
) : IConnectTimeoutPolicy {
    private var connectTimeOut = 0

    init {
        updateParams(true)
    }

    override fun getConnectTimeOut(): Int {
        return connectTimeOut
    }

    override fun getHandshakeTimeOut(): Int {
        return connectTimeoutConfig.sslHandshakeTimeOut
    }

    override fun updateParams(isSslPort: Boolean) {
        connectTimeOut = if (isSslPort) {
            connectTimeoutConfig.sslUpperBoundConnTimeOut
        } else {
            connectTimeoutConfig.upperBoundConnTimeOut
        }
    }

    override fun toString(): String {
        return " connectTimeOut : $connectTimeOut"
    }

    override fun resetParams() {
        connectTimeOut = 0
        updateParams(true)
    }
}
