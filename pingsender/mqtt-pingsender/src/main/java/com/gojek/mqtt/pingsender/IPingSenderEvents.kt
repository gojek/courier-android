package com.gojek.mqtt.pingsender

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY)
interface IPingSenderEvents {
    fun exceptionInStart(e: Exception)
    fun mqttPingScheduled(nextPingTimeSecs: Long, keepAliveSecs: Long)
    fun mqttPingInitiated(serverUri: String, keepAliveSecs: Long)
    fun pingMqttTokenNull(serverUri: String, keepAliveSecs: Long)
    fun pingEventSuccess(serverUri: String, timeTaken: Long, keepAliveSecs: Long)
    fun pingEventFailure(serverUri: String, timeTaken: Long, exception: Throwable, keepAliveSecs: Long)
    fun onBackgroundAlarmPingLimitReached()
}
