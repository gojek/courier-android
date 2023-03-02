package com.gojek.mqtt.client.config.v3

import com.gojek.courier.logging.ILogger
import com.gojek.courier.logging.NoOpLogger
import com.gojek.mqtt.auth.Authenticator
import com.gojek.mqtt.client.MqttInterceptor
import com.gojek.mqtt.client.config.ExperimentConfigs
import com.gojek.mqtt.client.config.MqttConfiguration
import com.gojek.mqtt.client.config.PersistenceOptions
import com.gojek.mqtt.client.config.PersistenceOptions.PahoPersistenceOptions
import com.gojek.mqtt.constants.DEFAULT_WAKELOCK_TIMEOUT
import com.gojek.mqtt.exception.handler.v3.AuthFailureHandler
import com.gojek.mqtt.pingsender.MqttPingSender
import com.gojek.mqtt.policies.connectretrytime.ConnectRetryTimeConfig
import com.gojek.mqtt.policies.connectretrytime.ConnectRetryTimePolicy
import com.gojek.mqtt.policies.connectretrytime.IConnectRetryTimePolicy
import com.gojek.mqtt.policies.connecttimeout.ConnectTimeoutConfig
import com.gojek.mqtt.policies.connecttimeout.ConnectTimeoutPolicy
import com.gojek.mqtt.policies.connecttimeout.IConnectTimeoutPolicy
import com.gojek.mqtt.policies.subscriptionretry.ISubscriptionRetryPolicy
import com.gojek.mqtt.policies.subscriptionretry.SubscriptionRetryConfig
import com.gojek.mqtt.policies.subscriptionretry.SubscriptionRetryPolicy

data class MqttV3Configuration(
    override val connectRetryTimePolicy: IConnectRetryTimePolicy =
        ConnectRetryTimePolicy(ConnectRetryTimeConfig()),
    override val connectTimeoutPolicy: IConnectTimeoutPolicy =
        ConnectTimeoutPolicy(ConnectTimeoutConfig()),
    override val subscriptionRetryPolicy: ISubscriptionRetryPolicy =
        SubscriptionRetryPolicy(SubscriptionRetryConfig()),
    override val unsubscriptionRetryPolicy: ISubscriptionRetryPolicy =
        SubscriptionRetryPolicy(SubscriptionRetryConfig()),
    override val wakeLockTimeout: Int = DEFAULT_WAKELOCK_TIMEOUT,
    override val logger: ILogger = NoOpLogger(),
    override val authenticator: Authenticator,
    override val authFailureHandler: AuthFailureHandler? = null,
    override val pingSender: MqttPingSender,
    override val mqttInterceptorList: List<MqttInterceptor> = emptyList(),
    override val persistenceOptions: PersistenceOptions = PahoPersistenceOptions(),
    override val experimentConfigs: ExperimentConfigs = ExperimentConfigs()
) : MqttConfiguration(
    connectRetryTimePolicy = connectRetryTimePolicy,
    connectTimeoutPolicy = connectTimeoutPolicy,
    subscriptionRetryPolicy = subscriptionRetryPolicy,
    unsubscriptionRetryPolicy = unsubscriptionRetryPolicy,
    wakeLockTimeout = wakeLockTimeout,
    logger = logger,
    authenticator = authenticator,
    authFailureHandler = authFailureHandler,
    pingSender = pingSender,
    mqttInterceptorList = mqttInterceptorList,
    persistenceOptions = persistenceOptions,
    experimentConfigs = experimentConfigs
)
