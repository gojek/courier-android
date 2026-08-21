package com.gojek.mqtt.topic

import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.junit.Assert.assertEquals
import org.junit.Test

class TopicPlaceholderResolverTest {

    private val resolver = TopicPlaceholderResolver()

    private fun optionsWithUsername(username: String): MqttConnectOptions {
        return MqttConnectOptions().apply { userName = username }
    }

    @Test
    fun `returns topic unchanged when it has no placeholder`() {
        val resolved = resolver.resolve(
            topic = "chat/room/inbox",
            options = optionsWithUsername("john"),
            clientId = "region:john:device"
        )

        assertEquals("chat/room/inbox", resolved)
    }

    @Test
    fun `resolves username placeholder`() {
        val resolved = resolver.resolve(
            topic = "chat/%u/inbox",
            options = optionsWithUsername("john"),
            clientId = "region:john:device"
        )

        assertEquals("chat/john/inbox", resolved)
    }

    @Test
    fun `resolves client id placeholder`() {
        val resolved = resolver.resolve(
            topic = "chat/%c/inbox",
            options = optionsWithUsername("john"),
            clientId = "region:john:device"
        )

        assertEquals("chat/region:john:device/inbox", resolved)
    }

    @Test
    fun `resolves split client id placeholder using the 0-based part index`() {
        val resolved = resolver.resolve(
            topic = "chat/(%c,:,1)/inbox",
            options = optionsWithUsername("john"),
            clientId = "region:john:device"
        )

        assertEquals("chat/john/inbox", resolved)
    }

    @Test
    fun `resolves split placeholder for the first part`() {
        val resolved = resolver.resolve(
            topic = "(%c,:,0)/inbox",
            options = optionsWithUsername("john"),
            clientId = "region:john:device"
        )

        assertEquals("region/inbox", resolved)
    }

    @Test
    fun `resolves split username placeholder`() {
        val resolved = resolver.resolve(
            topic = "chat/(%u,@,0)/inbox",
            options = optionsWithUsername("john@gojek.com"),
            clientId = "region:john:device"
        )

        assertEquals("chat/john/inbox", resolved)
    }

    @Test
    fun `does not corrupt split placeholder when plain placeholder also present`() {
        val resolved = resolver.resolve(
            topic = "(%c,:,1)/%c",
            options = optionsWithUsername("john"),
            clientId = "region:john:device"
        )

        assertEquals("john/region:john:device", resolved)
    }

    @Test
    fun `resolves multiple split placeholders in a single topic`() {
        val resolved = resolver.resolve(
            topic = "(%c,:,0)/(%c,:,2)",
            options = optionsWithUsername("john"),
            clientId = "region:john:device"
        )

        assertEquals("region/device", resolved)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `throws when split part index is out of bounds`() {
        resolver.resolve(
            topic = "chat/(%c,:,3)/inbox",
            options = optionsWithUsername("john"),
            clientId = "region:john:device"
        )
    }
}
