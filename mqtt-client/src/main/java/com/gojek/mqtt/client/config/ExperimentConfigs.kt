package com.gojek.mqtt.client.config

import com.gojek.mqtt.constants.DEFAULT_ACTIVITY_CHECK_INTERVAL_SECS
import com.gojek.mqtt.constants.DEFAULT_INACTIVITY_TIMEOUT_SECS
import com.gojek.mqtt.constants.DEFAULT_POLICY_RESET_TIME_SECS
import com.gojek.mqtt.model.AdaptiveKeepAliveConfig

data class ExperimentConfigs(
    val shouldConnectOnForeground: Boolean = true,
    val shouldConnectOnBackground: Boolean = true,
    val isPersistentSubscriptionStoreEnabled: Boolean = true,
    val adaptiveKeepAliveConfig: AdaptiveKeepAliveConfig? = null,
    val activityCheckIntervalSeconds: Int = DEFAULT_ACTIVITY_CHECK_INTERVAL_SECS,
    val inactivityTimeoutSeconds: Int = DEFAULT_INACTIVITY_TIMEOUT_SECS,
    val policyResetTimeSeconds: Int = DEFAULT_POLICY_RESET_TIME_SECS,
    val isMqttVersion4Enabled: Boolean = false,
    val incomingMessagesTTLSecs: Long = 360,
    val incomingMessagesCleanupIntervalSecs: Long = 60,
)
