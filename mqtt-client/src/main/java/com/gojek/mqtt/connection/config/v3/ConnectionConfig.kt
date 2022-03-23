package com.gojek.mqtt.connection.config.v3

import com.gojek.courier.logging.ILogger
import com.gojek.mqtt.constants.DISCONNECT_TIMEOUT_MILLIS
import com.gojek.mqtt.constants.QUIESCE_TIME_MILLIS
import com.gojek.mqtt.client.config.PersistenceOptions
import com.gojek.mqtt.connection.event.ConnectionEventHandler
import com.gojek.mqtt.policies.connectretrytime.IConnectRetryTimePolicy
import com.gojek.mqtt.policies.connecttimeout.IConnectTimeoutPolicy
import com.gojek.mqtt.policies.subscriptionretry.ISubscriptionRetryPolicy
import org.eclipse.paho.client.mqttv3.MqttInterceptor
import javax.net.SocketFactory

internal data class ConnectionConfig(
    val connectRetryTimePolicy: IConnectRetryTimePolicy,
    val connectTimeoutPolicy: IConnectTimeoutPolicy,
    val subscriptionRetryPolicy: ISubscriptionRetryPolicy,
    val unsubscriptionRetryPolicy: ISubscriptionRetryPolicy,
    val wakeLockTimeout: Int,
    val maxInflightMessages: Int,
    val logger: ILogger,
    val socketFactory: SocketFactory?,
    val connectionEventHandler: ConnectionEventHandler,
    val quiesceTimeout: Int = QUIESCE_TIME_MILLIS,
    val disconnectTimeout: Int = DISCONNECT_TIMEOUT_MILLIS,
    val mqttInterceptorList: List<MqttInterceptor>,
    val persistenceOptions: PersistenceOptions,
    val inactivityTimeoutSeconds: Int,
    val policyResetTimeSeconds: Int,
    val isMqttVersion4Enabled: Boolean,
)