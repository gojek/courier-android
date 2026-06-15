package com.gojek.mqtt.pingsender

import androidx.annotation.RestrictTo

/**
 * Paho-agnostic facade over the MQTT client connection internals required by
 * [MqttPingSender] implementations. Concrete adapters in the mqtt-client module
 * bridge this to the underlying Paho v3 / v5 `ClientComms`.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
interface PingSenderComms {
    /**
     * Client identifier of the active connection, or null if unavailable.
     */
    val clientId: String?

    /**
     * Server URI of the active connection, or null if unavailable.
     */
    val serverURI: String?

    /**
     * Keep alive interval in milliseconds.
     */
    val keepAliveMillis: Long

    /**
     * Checks for inbound/outbound activity and sends a ping if required.
     *
     * @param forcePing when true, a ping is sent regardless of recent activity.
     * @param callback invoked asynchronously with the ping result.
     * @return true if a ping was initiated (callback will fire), false otherwise.
     */
    fun checkActivity(forcePing: Boolean, callback: PingActionCallback): Boolean

    /**
     * Sends a ping request unconditionally.
     *
     * @param callback invoked asynchronously with the ping result.
     * @return true if a ping was initiated (callback will fire), false otherwise.
     */
    fun sendPingRequest(callback: PingActionCallback): Boolean

    /**
     * Records the time at which the app may be killed, used for diagnostics.
     * No-op on Paho versions that do not support this.
     */
    fun setAppKillTime(time: Long)
}
