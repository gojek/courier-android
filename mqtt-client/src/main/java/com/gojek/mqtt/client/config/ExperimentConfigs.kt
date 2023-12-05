package com.gojek.mqtt.client.config

import com.gojek.mqtt.client.config.SubscriptionStore.PERSISTABLE
import com.gojek.mqtt.constants.DEFAULT_ACTIVITY_CHECK_INTERVAL_SECS
import com.gojek.mqtt.constants.DEFAULT_INACTIVITY_TIMEOUT_SECS
import com.gojek.mqtt.constants.DEFAULT_POLICY_RESET_TIME_SECS
import com.gojek.mqtt.constants.MAX_INFLIGHT_MESSAGES_ALLOWED
import com.gojek.mqtt.model.AdaptiveKeepAliveConfig

data class ExperimentConfigs(
    val subscriptionStore: SubscriptionStore = PERSISTABLE,
    val adaptiveKeepAliveConfig: AdaptiveKeepAliveConfig? = null,
    val activityCheckIntervalSeconds: Int = DEFAULT_ACTIVITY_CHECK_INTERVAL_SECS,
    val inactivityTimeoutSeconds: Int = DEFAULT_INACTIVITY_TIMEOUT_SECS,
    val connectPacketTimeoutSeconds: Int = DEFAULT_INACTIVITY_TIMEOUT_SECS,
    val policyResetTimeSeconds: Int = DEFAULT_POLICY_RESET_TIME_SECS,
    val incomingMessagesTTLSecs: Long = 360,
    val incomingMessagesCleanupIntervalSecs: Long = 60,
    val shouldUseNewSSLFlow: Boolean = false,
    val maxInflightMessagesLimit: Int = MAX_INFLIGHT_MESSAGES_ALLOWED,
    val cleanMqttClientOnDestroy: Boolean = false
)

enum class SubscriptionStore {
    IN_MEMORY, PERSISTABLE, PERSISTABLE_V2
}
