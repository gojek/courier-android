package com.gojek.mqtt.policies.subscriptionretry

import com.gojek.mqtt.policies.IFallbackPolicy

interface ISubscriptionRetryPolicy : IFallbackPolicy {
    fun shouldRetry(): Boolean
}
