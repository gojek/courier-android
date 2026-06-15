package com.gojek.mqtt.connection

import org.eclipse.paho.client.mqttv3.ConnectionSpec
import org.eclipse.paho.client.mqttv3.Protocol
import org.eclipse.paho.mqttv5.client.ConnectionSpec as V5ConnectionSpec
import org.eclipse.paho.mqttv5.client.Protocol as V5Protocol

/**
 * Converts a public (MQTT v3 packaged) [ConnectionSpec] into the equivalent
 * MQTT v5 packaged spec. The public Courier API exposes the v3 paho TLS types, so
 * the v5 connection backend must translate them.
 */
internal fun ConnectionSpec.toV5ConnectionSpec(): V5ConnectionSpec {
    if (!isTls) {
        return V5ConnectionSpec.CLEARTEXT
    }
    return V5ConnectionSpec.create(
        true,
        supportsTlsExtensions,
        cipherSuites?.map { it.javaName }?.toTypedArray(),
        tlsVersions?.map { it.javaName }?.toTypedArray()
    )
}

/**
 * Converts a public (MQTT v3 packaged) [Protocol] into the MQTT v5 packaged
 * [V5Protocol].
 */
internal fun Protocol.toV5Protocol(): V5Protocol = V5Protocol(name)
