package com.gojek.mqtt.client.config

import com.gojek.mqtt.constants.DEFAULT_ACTIVITY_CHECK_INTERVAL_SECS
import com.gojek.mqtt.constants.DEFAULT_INACTIVITY_TIMEOUT_SECS
import com.gojek.mqtt.constants.DEFAULT_POLICY_RESET_TIME_SECS
import com.gojek.mqtt.model.AdaptiveKeepAliveConfig

data class ExperimentConfigs(
    val shouldConnectOnForeground: Boolean = true,
    val shouldConnectOnBackground: Boolean = true,
    val isNewSubscriptionStoreEnabled: Boolean = false,
    val isNetworkCheckEnabled: Boolean = true,
    val isNetworkValidatedCheckEnabled: Boolean = true,
    val extendedUsernameConfig: ExtendedUsernameConfig? = null,
    val adaptiveKeepAliveConfig: AdaptiveKeepAliveConfig? = null,
    val pingExperimentVariant: Int = 1,
    val activityCheckIntervalSeconds: Int = DEFAULT_ACTIVITY_CHECK_INTERVAL_SECS,
    val inactivityTimeoutSeconds: Int = DEFAULT_INACTIVITY_TIMEOUT_SECS,
    val policyResetTimeSeconds: Int = DEFAULT_POLICY_RESET_TIME_SECS,
    val removeConnectionCheckRunnableAfterDisconnect: Boolean = false,
    val isNewStoreLogicEnabled: Boolean = true,
    val isMqttVersion4Enabled: Boolean = false,
)

data class ExtendedUsernameConfig(
    val countryCode: String
)