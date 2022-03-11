package com.gojek.mqtt.policies.connectretrytime

data class ConnectRetryTimeConfig(
    val maxRetryCount: Int = MAX_RETRY_COUNT_DEFAULT,
    val reconnectTimeFixed: Int = RECONNECT_TIME_FIXED_DEFAULT,
    val reconnectTimeRandom: Int = RECONNECT_TIME_RANDOM_DEFAULT,
    val maxReconnectTime: Int = MAX_RECONNECT_TIME_DEFAULT
) {
    companion object {
        /* the maximum tries after which retry time starts increasing exponentially*/
        const val MAX_RETRY_COUNT_DEFAULT = 10

        /* how often to ping after a failure(fixed component)*/
        const val RECONNECT_TIME_FIXED_DEFAULT = 5 /* 5 seconds */

        /* how often to ping after a failure(random component) */
        const val RECONNECT_TIME_RANDOM_DEFAULT = 15 /* 15 seconds */

        /* the max amount (in seconds) the reconnect time can be */
        const val MAX_RECONNECT_TIME_DEFAULT = 180
    }
}