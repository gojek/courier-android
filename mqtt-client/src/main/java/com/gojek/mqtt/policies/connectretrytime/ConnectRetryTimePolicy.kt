package com.gojek.mqtt.policies.connectretrytime

import java.util.Random
import java.util.concurrent.atomic.AtomicInteger

class ConnectRetryTimePolicy(
    private val connectRetryTimeConfig: ConnectRetryTimeConfig
) : IConnectRetryTimePolicy {
    private val reconnectTime: AtomicInteger = AtomicInteger(0)
    private val retryCount: AtomicInteger = AtomicInteger(0)

    override fun getConnRetryTimeSecs(isAuthFailure: Boolean): Int {
        if (isAuthFailure) {
            return 0
        }
        val maxRetryCount = connectRetryTimeConfig.maxRetryCount
        val reconnectTimeFixed = connectRetryTimeConfig.reconnectTimeFixed
        val reconnectTimeRandom = connectRetryTimeConfig.reconnectTimeRandom
        val maxReconnectTime = connectRetryTimeConfig.maxReconnectTime
        val random = Random()
        if ((reconnectTime.get() == 0 || retryCount.get() < maxRetryCount)) {
            reconnectTime.set(reconnectTimeFixed + random.nextInt(reconnectTimeRandom) + 1)
            retryCount.getAndIncrement()
        } else {
            updateReconnectTimeExponentially(2)
        }
        if (reconnectTime.get() > maxReconnectTime) {
            reconnectTime.set(maxReconnectTime)
        } else if (reconnectTime.get() == 0) {
            // if reconnectTime is 0, select the random value.
            // This will happen in case of forceExp = true
            reconnectTime.set(reconnectTimeFixed + random.nextInt(reconnectTimeRandom) + 1)
        }
        return reconnectTime.get()
    }

    private fun updateReconnectTimeExponentially(exponentialFactor: Int): Int {
        while (true) {
            val current = reconnectTime.get()
            val next = current * exponentialFactor
            if (reconnectTime.compareAndSet(current, next)) {
                return next
            }
        }
    }

    override fun resetParams() {
        reconnectTime.set(0)
        retryCount.set(0)
    }
}
