package com.gojek.mqtt.policies.subscriptionretry

data class SubscriptionRetryConfig(
    val maxRetryCount: Int = DEFAULT_MAX_RETRY_COUNT
) {

    companion object {
        const val DEFAULT_MAX_RETRY_COUNT = 3
    }
}
