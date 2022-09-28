package com.gojek.mqtt.client.config

import com.gojek.courier.logging.ILogger
import com.gojek.mqtt.auth.Authenticator
import com.gojek.mqtt.client.MqttInterceptor
import com.gojek.mqtt.event.EventHandler
import com.gojek.mqtt.exception.handler.v3.AuthFailureHandler
import com.gojek.mqtt.pingsender.MqttPingSender
import com.gojek.mqtt.policies.connectretrytime.IConnectRetryTimePolicy
import com.gojek.mqtt.policies.connecttimeout.IConnectTimeoutPolicy
import com.gojek.mqtt.policies.subscriptionretry.ISubscriptionRetryPolicy

abstract class MqttConfiguration(
    open val connectRetryTimePolicy: IConnectRetryTimePolicy,
    open val connectTimeoutPolicy: IConnectTimeoutPolicy,
    open val subscriptionRetryPolicy: ISubscriptionRetryPolicy,
    open val unsubscriptionRetryPolicy: ISubscriptionRetryPolicy,
    open val wakeLockTimeout: Int,
    open val logger: ILogger,
    open val authenticator: Authenticator,
    open val authFailureHandler: AuthFailureHandler?,
    open val eventHandler: EventHandler,
    open val pingSender: MqttPingSender,
    open val mqttInterceptorList: List<MqttInterceptor>,
    open val persistenceOptions: PersistenceOptions,
    open val experimentConfigs: ExperimentConfigs
)
