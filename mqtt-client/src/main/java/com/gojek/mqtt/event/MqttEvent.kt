package com.gojek.mqtt.event

import com.gojek.courier.QoS
import com.gojek.mqtt.client.connectioninfo.ConnectionInfo
import com.gojek.mqtt.exception.CourierException
import com.gojek.mqtt.model.MqttConnectOptions
import com.gojek.mqtt.model.ServerUri
import com.gojek.mqtt.network.ActiveNetInfo

sealed class MqttEvent(open var connectionInfo: ConnectionInfo?) {
    data class MqttConnectAttemptEvent(
        val isOptimalKeepAlive: Boolean,
        val activeNetInfo: ActiveNetInfo,
        val serverUri: ServerUri?,
        override var connectionInfo: ConnectionInfo? = null
    ) : MqttEvent(connectionInfo)

    data class MqttConnectDiscardedEvent(
        val reason: String,
        val activeNetworkInfo: ActiveNetInfo,
        override var connectionInfo: ConnectionInfo? = null
    ) : MqttEvent(connectionInfo)

    data class MqttConnectSuccessEvent(
        val activeNetInfo: ActiveNetInfo,
        val serverUri: ServerUri?,
        val timeTakenMillis: Long,
        val connectPacketRTTime: Long,
        override var connectionInfo: ConnectionInfo? = null
    ) : MqttEvent(connectionInfo)

    data class MqttConnectFailureEvent(
        val exception: CourierException,
        val activeNetInfo: ActiveNetInfo,
        val serverUri: ServerUri?,
        val timeTakenMillis: Long,
        override var connectionInfo: ConnectionInfo? = null
    ) : MqttEvent(connectionInfo)

    data class MqttConnectionLostEvent(
        val exception: CourierException,
        val activeNetInfo: ActiveNetInfo,
        val serverUri: ServerUri?,
        val nextRetryTimeSecs: Int,
        val sessionTimeMillis: Long,
        override var connectionInfo: ConnectionInfo? = null
    ) : MqttEvent(connectionInfo)

    data class SocketConnectAttemptEvent(
        val port: Int,
        val host: String?,
        val timeout: Long,
        override var connectionInfo: ConnectionInfo? = null
    ) : MqttEvent(connectionInfo)

    data class SocketConnectSuccessEvent(
        val port: Int,
        val host: String?,
        val timeout: Long,
        val timeTakenMillis: Long,
        override var connectionInfo: ConnectionInfo? = null
    ) : MqttEvent(connectionInfo)

    data class SocketConnectFailureEvent(
        val port: Int,
        val host: String?,
        val timeout: Long,
        val timeTakenMillis: Long,
        val exception: CourierException,
        override var connectionInfo: ConnectionInfo? = null
    ) : MqttEvent(connectionInfo)

    data class SSLSocketAttemptEvent(
        val port: Int,
        val host: String?,
        val timeout: Long,
        override var connectionInfo: ConnectionInfo? = null
    ) : MqttEvent(connectionInfo)

    data class SSLSocketSuccessEvent(
        val port: Int,
        val host: String?,
        val timeout: Long,
        val timeTakenMillis: Long,
        override var connectionInfo: ConnectionInfo? = null
    ) : MqttEvent(connectionInfo)

    data class SSLSocketFailureEvent(
        val port: Int,
        val host: String?,
        val timeout: Long,
        val exception: CourierException,
        val timeTakenMillis: Long,
        override var connectionInfo: ConnectionInfo? = null
    ) : MqttEvent(connectionInfo)

    data class SSLHandshakeSuccessEvent(
        val port: Int,
        val host: String?,
        val timeout: Long,
        val timeTakenMillis: Long,
        override var connectionInfo: ConnectionInfo? = null
    ) : MqttEvent(connectionInfo)

    data class ConnectPacketSendEvent(
        override var connectionInfo: ConnectionInfo? = null
    ) : MqttEvent(connectionInfo)

    data class MqttSubscribeAttemptEvent(
        val topics: Map<String, QoS>,
        override var connectionInfo: ConnectionInfo? = null
    ) : MqttEvent(connectionInfo)

    data class MqttSubscribeSuccessEvent(
        val topics: Map<String, QoS>,
        val timeTakenMillis: Long,
        override var connectionInfo: ConnectionInfo? = null
    ) : MqttEvent(connectionInfo)

    data class MqttSubscribeFailureEvent(
        val topics: Map<String, QoS>,
        val exception: CourierException,
        val timeTakenMillis: Long,
        override var connectionInfo: ConnectionInfo? = null
    ) : MqttEvent(connectionInfo)

    data class MqttUnsubscribeAttemptEvent(
        val topics: Set<String>,
        override var connectionInfo: ConnectionInfo? = null
    ) : MqttEvent(connectionInfo)

    data class MqttUnsubscribeSuccessEvent(
        val topics: Set<String>,
        val timeTakenMillis: Long,
        override var connectionInfo: ConnectionInfo? = null
    ) : MqttEvent(connectionInfo)

    data class MqttUnsubscribeFailureEvent(
        val topics: Set<String>,
        val exception: CourierException,
        val timeTakenMillis: Long,
        override var connectionInfo: ConnectionInfo? = null
    ) : MqttEvent(connectionInfo)

    data class MqttMessageReceiveEvent(
        val topic: String,
        val sizeBytes: Int,
        override var connectionInfo: ConnectionInfo? = null
    ) : MqttEvent(connectionInfo)

    data class MqttMessageReceiveErrorEvent(
        val topic: String,
        val sizeBytes: Int,
        val exception: CourierException,
        override var connectionInfo: ConnectionInfo? = null
    ) : MqttEvent(connectionInfo)

    data class MqttMessageSendEvent(
        val topic: String,
        val qos: Int,
        val sizeBytes: Int,
        override var connectionInfo: ConnectionInfo? = null
    ) : MqttEvent(connectionInfo)

    data class MqttMessageSendSuccessEvent(
        val topic: String,
        val qos: Int,
        val sizeBytes: Int,
        val timeTakenMillis: Long,
        override var connectionInfo: ConnectionInfo? = null
    ) : MqttEvent(connectionInfo)

    data class MqttMessageSendFailureEvent(
        val topic: String,
        val qos: Int,
        val sizeBytes: Int,
        val exception: CourierException,
        val timeTakenMillis: Long,
        override var connectionInfo: ConnectionInfo? = null
    ) : MqttEvent(connectionInfo)

    data class MqttPingInitiatedEvent(
        val serverUri: String,
        val keepAliveSecs: Long,
        val isAdaptive: Boolean,
        override var connectionInfo: ConnectionInfo? = null
    ) : MqttEvent(connectionInfo)

    data class MqttPingScheduledEvent(
        val nextPingTimeSecs: Long,
        val keepAliveSecs: Long,
        val isAdaptive: Boolean,
        override var connectionInfo: ConnectionInfo? = null
    ) : MqttEvent(connectionInfo)

    data class MqttPingCancelledEvent(
        val serverUri: String,
        val keepAliveSecs: Long,
        val isAdaptive: Boolean,
        override var connectionInfo: ConnectionInfo? = null
    ) : MqttEvent(connectionInfo)

    data class MqttPingSuccessEvent(
        val serverUri: String,
        val timeTakenMillis: Long,
        val keepAliveSecs: Long,
        val isAdaptive: Boolean,
        override var connectionInfo: ConnectionInfo? = null
    ) : MqttEvent(connectionInfo)

    data class MqttPingFailureEvent(
        val serverUri: String,
        val timeTakenMillis: Long,
        val keepAliveSecs: Long,
        val exception: CourierException,
        val isAdaptive: Boolean,
        override var connectionInfo: ConnectionInfo? = null
    ) : MqttEvent(connectionInfo)

    data class MqttPingExceptionEvent(
        val exception: CourierException,
        val isAdaptive: Boolean,
        override var connectionInfo: ConnectionInfo? = null
    ) : MqttEvent(connectionInfo)

    data class BackgroundAlarmPingLimitReached(
        val isAdaptive: Boolean,
        override var connectionInfo: ConnectionInfo? = null
    ) : MqttEvent(connectionInfo)

    data class OptimalKeepAliveFoundEvent(
        val timeMinutes: Int,
        val probeCount: Int,
        val convergenceTime: Int,
        override var connectionInfo: ConnectionInfo? = null
    ) : MqttEvent(connectionInfo)

    data class MqttReconnectEvent(
        override var connectionInfo: ConnectionInfo? = null
    ) : MqttEvent(connectionInfo)

    data class MqttDisconnectEvent(
        override var connectionInfo: ConnectionInfo? = null
    ) : MqttEvent(connectionInfo)

    data class MqttDisconnectStartEvent(
        override var connectionInfo: ConnectionInfo? = null
    ) : MqttEvent(connectionInfo)

    data class MqttDisconnectCompleteEvent(
        override var connectionInfo: ConnectionInfo? = null
    ) : MqttEvent(connectionInfo)

    data class MqttClientDestroyedEvent(
        override var connectionInfo: ConnectionInfo? = null
    ) : MqttEvent(connectionInfo)

    data class OfflineMessageDiscardedEvent(
        val messageId: Int,
        override var connectionInfo: ConnectionInfo? = null
    ) : MqttEvent(connectionInfo)

    data class InboundInactivityEvent(
        override var connectionInfo: ConnectionInfo? = null
    ) : MqttEvent(connectionInfo)

    data class HandlerThreadNotAliveEvent(
        val isInterrupted: Boolean,
        val state: Thread.State,
        override var connectionInfo: ConnectionInfo? = null
    ) : MqttEvent(connectionInfo)

    data class AuthenticatorAttemptEvent(
        val forceRefresh: Boolean,
        val connectOptions: MqttConnectOptions,
        override var connectionInfo: ConnectionInfo? = null
    ) : MqttEvent(connectionInfo)

    data class AuthenticatorSuccessEvent(
        val forceRefresh: Boolean,
        val connectOptions: MqttConnectOptions,
        val timeTakenMillis: Long,
        override var connectionInfo: ConnectionInfo? = null
    ) : MqttEvent(connectionInfo)

    data class AuthenticatorErrorEvent(
        val exception: CourierException,
        val nextRetryTimeSecs: Long,
        val activeNetworkInfo: ActiveNetInfo,
        val timeTakenMillis: Long,
        override var connectionInfo: ConnectionInfo? = null
    ) : MqttEvent(connectionInfo)

    data class OperationDiscardedEvent(
        val name: String,
        val reason: String,
        override var connectionInfo: ConnectionInfo? = null
    ) : MqttEvent(connectionInfo)
}
