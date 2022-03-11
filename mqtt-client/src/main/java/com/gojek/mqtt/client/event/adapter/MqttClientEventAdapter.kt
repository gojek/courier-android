package com.gojek.mqtt.client.event.adapter

import com.gojek.courier.QoS
import com.gojek.mqtt.connection.event.ConnectionEventHandler
import com.gojek.mqtt.event.EventHandler
import com.gojek.mqtt.event.MqttEvent.ConnectPacketSendEvent
import com.gojek.mqtt.event.MqttEvent.InboundInactivityEvent
import com.gojek.mqtt.event.MqttEvent.MqttConnectAttemptEvent
import com.gojek.mqtt.event.MqttEvent.MqttConnectDiscardedEvent
import com.gojek.mqtt.event.MqttEvent.MqttConnectFailureEvent
import com.gojek.mqtt.event.MqttEvent.MqttConnectSuccessEvent
import com.gojek.mqtt.event.MqttEvent.MqttConnectionLostEvent
import com.gojek.mqtt.event.MqttEvent.MqttDisconnectCompleteEvent
import com.gojek.mqtt.event.MqttEvent.MqttDisconnectStartEvent
import com.gojek.mqtt.event.MqttEvent.MqttSubscribeAttemptEvent
import com.gojek.mqtt.event.MqttEvent.MqttSubscribeFailureEvent
import com.gojek.mqtt.event.MqttEvent.MqttSubscribeSuccessEvent
import com.gojek.mqtt.event.MqttEvent.MqttUnsubscribeAttemptEvent
import com.gojek.mqtt.event.MqttEvent.MqttUnsubscribeFailureEvent
import com.gojek.mqtt.event.MqttEvent.MqttUnsubscribeSuccessEvent
import com.gojek.mqtt.event.MqttEvent.OfflineMessageDiscardedEvent
import com.gojek.mqtt.event.MqttEvent.SSLHandshakeSuccessEvent
import com.gojek.mqtt.event.MqttEvent.SSLSocketAttemptEvent
import com.gojek.mqtt.event.MqttEvent.SSLSocketFailureEvent
import com.gojek.mqtt.event.MqttEvent.SSLSocketSuccessEvent
import com.gojek.mqtt.event.MqttEvent.SocketConnectAttemptEvent
import com.gojek.mqtt.event.MqttEvent.SocketConnectFailureEvent
import com.gojek.mqtt.event.MqttEvent.SocketConnectSuccessEvent
import com.gojek.mqtt.exception.CourierException
import com.gojek.mqtt.exception.toCourierException
import com.gojek.mqtt.model.ServerUri
import com.gojek.mqtt.network.NetworkHandler

internal class MqttClientEventAdapter(
    private val eventHandler: EventHandler,
    private val networkHandler: NetworkHandler
) {
    fun adapt(): ConnectionEventHandler {
        return object : ConnectionEventHandler {
            override fun onMqttConnectAttempt(
                isOptimalKeepAlive: Boolean,
                serverUri: ServerUri?
            ) {
                eventHandler.onEvent(
                    MqttConnectAttemptEvent(
                        isOptimalKeepAlive = isOptimalKeepAlive,
                        activeNetInfo = networkHandler.getActiveNetworkInfo(),
                        serverUri = serverUri
                    )
                )
            }

            override fun onMqttConnectSuccess(
                serverUri: ServerUri?,
                timeTakenMillis: Long
            ) {
                eventHandler.onEvent(
                    MqttConnectSuccessEvent(
                        activeNetInfo = networkHandler.getActiveNetworkInfo(),
                        serverUri = serverUri,
                        timeTakenMillis = timeTakenMillis
                    )
                )
            }

            override fun onMqttConnectFailure(
                throwable: Throwable,
                serverUri: ServerUri?,
                timeTakenMillis: Long
            ) {
                eventHandler.onEvent(
                    MqttConnectFailureEvent(
                        exception = throwable.toCourierException(),
                        activeNetInfo = networkHandler.getActiveNetworkInfo(),
                        serverUri = serverUri,
                        timeTakenMillis = timeTakenMillis
                    )
                )
            }

            override fun onMqttConnectionLost(
                throwable: Throwable,
                serverUri: ServerUri?,
                nextRetryTimeSecs: Int,
                sessionTimeMillis: Long
            ) {
                eventHandler.onEvent(
                    MqttConnectionLostEvent(
                        exception = throwable.toCourierException(),
                        activeNetInfo = networkHandler.getActiveNetworkInfo(),
                        serverUri = serverUri,
                        nextRetryTimeSecs = nextRetryTimeSecs,
                        sessionTimeMillis = sessionTimeMillis
                    )
                )
            }

            override fun onMqttSubscribeAttempt(topics: Map<String, QoS>) {
                eventHandler.onEvent(
                    MqttSubscribeAttemptEvent(
                        topics = topics
                    )
                )
            }

            override fun onMqttSubscribeSuccess(
                topics: Map<String, QoS>,
                timeTakenMillis: Long
            ) {
                eventHandler.onEvent(
                    MqttSubscribeSuccessEvent(
                        topics = topics,
                        timeTakenMillis = timeTakenMillis
                    )
                )
            }

            override fun onMqttSubscribeFailure(
                topics: Map<String, QoS>,
                throwable: Throwable,
                timeTakenMillis: Long
            ) {
                eventHandler.onEvent(
                    MqttSubscribeFailureEvent(
                        topics = topics,
                        exception = throwable.toCourierException(),
                        timeTakenMillis = timeTakenMillis
                    )
                )
            }

            override fun onMqttUnsubscribeFailure(
                topics: Set<String>,
                throwable: Throwable,
                timeTakenMillis: Long
            ) {
                eventHandler.onEvent(
                    MqttUnsubscribeFailureEvent(
                        topics = topics,
                        exception = throwable.toCourierException(),
                        timeTakenMillis = timeTakenMillis
                    )
                )
            }

            override fun onMqttUnsubscribeAttempt(topics: Set<String>) {
                eventHandler.onEvent(
                    MqttUnsubscribeAttemptEvent(
                        topics = topics
                    )
                )
            }

            override fun onMqttUnsubscribeSuccess(
                topics: Set<String>,
                timeTakenMillis: Long
            ) {
                eventHandler.onEvent(
                    MqttUnsubscribeSuccessEvent(
                        topics = topics,
                        timeTakenMillis = timeTakenMillis
                    )
                )
            }

            override fun onSocketConnectAttempt(port: Int, host: String?, timeout: Long) {
                eventHandler.onEvent(
                    SocketConnectAttemptEvent(
                        port = port,
                        host = host,
                        timeout = timeout
                    )
                )
            }

            override fun onSocketConnectSuccess(
                timeToConnect: Long,
                port: Int,
                host: String?,
                timeout: Long
            ) {
                eventHandler.onEvent(
                    SocketConnectSuccessEvent(
                        port = port,
                        host = host,
                        timeout = timeout,
                        timeTakenMillis = timeToConnect
                    )
                )
            }

            override fun onSocketConnectFailure(
                timeToConnect: Long,
                port: Int,
                host: String?,
                timeout: Long,
                throwable: Throwable?
            ) {
                eventHandler.onEvent(
                    SocketConnectFailureEvent(
                        port = port,
                        host = host,
                        timeout = timeout,
                        timeTakenMillis = timeToConnect,
                        exception = throwable.toCourierException()
                    )
                )
            }

            override fun onConnectPacketSend() {
                eventHandler.onEvent(ConnectPacketSendEvent())
            }

            override fun onSSLSocketAttempt(port: Int, host: String?, timeout: Long) {
                eventHandler.onEvent(
                    SSLSocketAttemptEvent(
                        port = port,
                        host = host,
                        timeout = timeout
                    )

                )
            }

            override fun onSSLSocketSuccess(
                port: Int,
                host: String?,
                timeout: Long,
                timeTakenMillis: Long
            ) {
                eventHandler.onEvent(
                    SSLSocketSuccessEvent(
                        port = port,
                        host = host,
                        timeout = timeout,
                        timeTakenMillis = timeTakenMillis
                    )
                )
            }

            override fun onSSLSocketFailure(
                port: Int,
                host: String?,
                timeout: Long,
                throwable: Throwable?,
                timeTakenMillis: Long
            ) {
                eventHandler.onEvent(
                    SSLSocketFailureEvent(
                        port = port,
                        host = host,
                        timeout = timeout,
                        exception = throwable.toCourierException(),
                        timeTakenMillis = timeTakenMillis
                    )
                )
            }

            override fun onSSLHandshakeSuccess(
                port: Int,
                host: String?,
                timeout: Long,
                timeTakenMillis: Long
            ) {
                eventHandler.onEvent(
                    SSLHandshakeSuccessEvent(
                        port = port,
                        host = host,
                        timeout = timeout,
                        timeTakenMillis = timeTakenMillis
                    )
                )
            }

            override fun onMqttDisconnectStart() {
                eventHandler.onEvent(MqttDisconnectStartEvent())
            }

            override fun onMqttDisconnectComplete() {
                eventHandler.onEvent(MqttDisconnectCompleteEvent())
            }

            override fun onMqttConnectDiscarded(reason: String) {
                eventHandler.onEvent(
                    MqttConnectDiscardedEvent(
                        reason = reason,
                        activeNetworkInfo = networkHandler.getActiveNetworkInfo()
                    )
                )
            }

            override fun onOfflineMessageDiscarded(messageId: Int) {
                eventHandler.onEvent(
                    OfflineMessageDiscardedEvent(messageId)
                )
            }

            override fun onInboundInactivity() {
                eventHandler.onEvent(
                    InboundInactivityEvent()
                )
            }
        }
    }
}