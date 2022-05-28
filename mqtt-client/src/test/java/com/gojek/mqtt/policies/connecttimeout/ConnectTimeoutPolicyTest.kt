package com.gojek.mqtt.policies.connecttimeout

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class ConnectTimeoutPolicyTest {
    private val connectTimeoutConfig = ConnectTimeoutConfig()
    private val connectTimeoutPolicy = ConnectTimeoutPolicy(connectTimeoutConfig)

    @Test
    fun `test getConnectTimeOut`() {
        assertEquals(
            connectTimeoutPolicy.getConnectTimeOut(), connectTimeoutConfig.sslUpperBoundConnTimeOut
        )
        connectTimeoutPolicy.updateParams(isSslPort = false)
        assertEquals(
            connectTimeoutPolicy.getConnectTimeOut(),
            connectTimeoutConfig.upperBoundConnTimeOut
        )
        connectTimeoutPolicy.updateParams(isSslPort = true)
        assertEquals(
            connectTimeoutPolicy.getConnectTimeOut(),
            connectTimeoutConfig.sslUpperBoundConnTimeOut
        )
        connectTimeoutPolicy.updateParams(isSslPort = false)
        assertEquals(
            connectTimeoutPolicy.getConnectTimeOut(),
            connectTimeoutConfig.upperBoundConnTimeOut
        )
        connectTimeoutPolicy.resetParams()
        assertEquals(
            connectTimeoutPolicy.getConnectTimeOut(),
            connectTimeoutConfig.sslUpperBoundConnTimeOut
        )
    }
}
