package com.gojek.mqtt.policies.subscriptionretry

import androidx.annotation.VisibleForTesting
import java.util.concurrent.atomic.AtomicInteger

class SubscriptionRetryPolicy(
    private val subscriptionRetryConfig: SubscriptionRetryConfig
) : ISubscriptionRetryPolicy {
    private val retryCount = AtomicInteger(0)

    override fun shouldRetry(): Boolean {
        return (retryCount.incrementAndGet() <= subscriptionRetryConfig.maxRetryCount)
    }

    override fun resetParams() {
        retryCount.set(0)
    }

    @VisibleForTesting
    internal fun getRetryCount(): Int {
        return retryCount.get()
    }
}
