package com.gojek.mqtt.pingsender

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY)
class NoOpPingSenderEvents : IPingSenderEvents {
    override fun exceptionInStart(e: Exception) {
    }

    override fun mqttPingScheduled(nextPingTimeSecs: Long, keepAliveSecs: Long) {
    }

    override fun mqttPingInitiated(serverUri: String, keepAliveSecs: Long) {
    }

    override fun pingMqttTokenNull(serverUri: String, keepAliveSecs: Long) {
    }

    override fun pingEventSuccess(serverUri: String, timeTaken: Long, keepAliveSecs: Long) {
    }

    override fun pingEventFailure(serverUri: String, timeTaken: Long, exception: Throwable, keepAliveSecs: Long) {
    }

    override fun onBackgroundAlarmPingLimitReached() {
    }
}
