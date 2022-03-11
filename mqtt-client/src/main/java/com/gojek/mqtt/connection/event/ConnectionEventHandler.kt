package com.gojek.mqtt.connection.event

import com.gojek.courier.QoS
import com.gojek.mqtt.model.ServerUri

internal interface ConnectionEventHandler {
    fun onMqttConnectAttempt(
        isOptimalKeepAlive: Boolean,
        serverUri: ServerUri?
    )
    fun onMqttConnectSuccess(
        serverUri: ServerUri?,
        timeTakenMillis: Long
    )
    fun onMqttConnectFailure(
        throwable: Throwable,
        serverUri: ServerUri?,
        timeTakenMillis: Long
    )
    fun onMqttConnectionLost(
        throwable: Throwable,
        serverUri: ServerUri?,
        nextRetryTimeSecs: Int,
        sessionTimeMillis: Long
    )

    fun onMqttSubscribeAttempt(topics: Map<String, QoS>)
    fun onMqttSubscribeSuccess(
        topics: Map<String, QoS>,
        timeTakenMillis: Long
    )
    fun onMqttSubscribeFailure(
        topics: Map<String, QoS>,
        throwable: Throwable,
        timeTakenMillis: Long
    )

    fun onMqttUnsubscribeFailure(
        topics: Set<String>,
        throwable: Throwable,
        timeTakenMillis: Long
    )
    fun onMqttUnsubscribeAttempt(topics: Set<String>)
    fun onMqttUnsubscribeSuccess(
        topics: Set<String>,
        timeTakenMillis: Long
    )

    fun onSocketConnectAttempt(
        port: Int,
        host: String?,
        timeout: Long
    )

    fun onSocketConnectSuccess(
        timeToConnect: Long,
        port: Int,
        host: String?,
        timeout: Long
    )

    fun onSocketConnectFailure(
        timeToConnect: Long,
        port: Int,
        host: String?,
        timeout: Long,
        throwable: Throwable?
    )

    fun onConnectPacketSend()

    fun onSSLSocketAttempt(
        port: Int,
        host: String?,
        timeout: Long
    )

    fun onSSLSocketSuccess(
        port: Int,
        host: String?,
        timeout: Long,
        timeTakenMillis: Long
    )

    fun onSSLSocketFailure(
        port: Int,
        host: String?,
        timeout: Long,
        throwable: Throwable?,
        timeTakenMillis: Long
    )

    fun onSSLHandshakeSuccess(
        port: Int,
        host: String?,
        timeout: Long,
        timeTakenMillis: Long
    )

    fun onMqttDisconnectStart()
    fun onMqttDisconnectComplete()
    fun onMqttConnectDiscarded(reason: String)
    fun onOfflineMessageDiscarded(messageId: Int)
    fun onInboundInactivity()
}