package com.gojek.mqtt.policies.connecttimeout

data class ConnectTimeoutConfig(
    val sslHandshakeTimeOut: Int = SSL_HANDSHAKE_TIMEOUT,
    val sslUpperBoundConnTimeOut: Int = UPPER_BOUND_CONN_TIMEOUT_SSL,
    val upperBoundConnTimeOut: Int = UPPER_BOUND_CONN_TIMEOUT
) {

    companion object {
        const val UPPER_BOUND_CONN_TIMEOUT_SSL = 120
        const val UPPER_BOUND_CONN_TIMEOUT = 60
        const val SSL_HANDSHAKE_TIMEOUT = 90
    }
}