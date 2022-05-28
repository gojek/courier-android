package com.gojek.mqtt.policies.hostfallback

import com.gojek.mqtt.model.ServerUri
import java.lang.IllegalArgumentException
import org.eclipse.paho.client.mqttv3.MqttException
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class HostFallbackPolicyTest {
    private lateinit var hostFallbackPolicy: HostFallbackPolicy

    @Test(expected = IllegalArgumentException::class)
    fun `test HostFallbackPolicy with empty serverUris list`() {
        hostFallbackPolicy = HostFallbackPolicy(emptyList())
    }

    @Test
    fun `test HostFallbackPolicy with non empty serverUris`() {
        val serverUris = listOf(
            ServerUri("test_uri", 1000),
            ServerUri("test_uri", 2000),
            ServerUri("test_uri2", 1000)
        )
        hostFallbackPolicy = HostFallbackPolicy(serverUris)

        // Test getServerUri should return the serverUri at index 0 for the first time
        assertEquals("ssl://test_uri:1000", hostFallbackPolicy.getServerUri().toString())

        // Test onConnectFailure with non MqttException should not change the index
        hostFallbackPolicy.onConnectFailure(Exception("Test"))
        assertEquals("ssl://test_uri:1000", hostFallbackPolicy.getServerUri().toString())

        // Test onConnectFailure with MqttException (rc != 0) should not change the index
        hostFallbackPolicy.onConnectFailure(MqttException(3000))
        assertEquals("ssl://test_uri:1000", hostFallbackPolicy.getServerUri().toString())

        // Test onConnectFailure with MqttException (rc = 0) should change the index
        hostFallbackPolicy.onConnectFailure(MqttException(0))
        assertEquals("ssl://test_uri:2000", hostFallbackPolicy.getServerUri().toString())

        // Test onConnectFailure with MqttException (rc = 0) should change the index
        hostFallbackPolicy.onConnectFailure(MqttException(0))
        assertEquals("ssl://test_uri2:1000", hostFallbackPolicy.getServerUri().toString())

        // Test onConnectFailure with MqttException (rc = 0) should bring the index to 0
        hostFallbackPolicy.onConnectFailure(MqttException(0))
        assertEquals("ssl://test_uri:1000", hostFallbackPolicy.getServerUri().toString())
    }

    @Test
    fun `test resetParams`() {
        val serverUris = listOf(
            ServerUri("test_uri", 1000),
            ServerUri("test_uri", 2000)
        )
        hostFallbackPolicy = HostFallbackPolicy(serverUris)
        assertEquals("ssl://test_uri:1000", hostFallbackPolicy.getServerUri().toString())

        hostFallbackPolicy.onConnectFailure(MqttException(0))
        assertEquals("ssl://test_uri:2000", hostFallbackPolicy.getServerUri().toString())

        hostFallbackPolicy.resetParams()
        assertEquals("ssl://test_uri:1000", hostFallbackPolicy.getServerUri().toString())
    }
}
