package com.gojek.mqtt.event

import com.gojek.mqtt.connection.event.ConnectionEventHandler
import org.eclipse.paho.client.mqttv3.IPahoEvents

internal class PahoEventHandler(
    private val connectionEventHandler: ConnectionEventHandler
): IPahoEvents {
    override fun onSSLSocketAttempt(port: Int, host: String?, timeout: Long) {
        connectionEventHandler.onSSLSocketAttempt(port, host, timeout)
    }

    override fun onConnectPacketSend() {
        connectionEventHandler.onConnectPacketSend()
    }

    override fun onSSLSocketFailure(
        port: Int,
        host: String?,
        timeout: Long,
        throwable: Throwable?,
        timeTakenMillis: Long
    ) {
        connectionEventHandler.onSSLSocketFailure(port, host, timeout, throwable, timeTakenMillis)
    }

    override fun onSSLSocketSuccess(
        port: Int,
        host: String?,
        timeout: Long,
        timeTakenMillis: Long
    ) {
        connectionEventHandler.onSSLSocketSuccess(port, host, timeout, timeTakenMillis)
    }

    override fun onSocketConnectFailure(
        timeToConnect: Long,
        port: Int,
        host: String?,
        timeout: Long,
        throwable: Throwable?
    ) {
        connectionEventHandler.onSocketConnectFailure(timeToConnect, port, host, timeout, throwable)
    }

    override fun onOfflineMessageDiscarded(messageId: Int) {
        connectionEventHandler.onOfflineMessageDiscarded(messageId)
    }

    override fun onSSLHandshakeSuccess(
        port: Int,
        host: String?,
        timeout: Long,
        timeTakenMillis: Long
    ) {
        connectionEventHandler.onSSLHandshakeSuccess(port, host, timeout, timeTakenMillis)
    }

    override fun onSocketConnectSuccess(
        timeToConnect: Long,
        port: Int,
        host: String?,
        timeout: Long
    ) {
        connectionEventHandler.onSocketConnectSuccess(timeToConnect, port, host, timeout)
    }

    override fun onSocketConnectAttempt(port: Int, host: String?, timeout: Long) {
        connectionEventHandler.onSocketConnectAttempt(port, host, timeout)
    }

    override fun onInboundInactivity() {
        connectionEventHandler.onInboundInactivity()
    }
}