package com.gojek.alarm.pingsender

data class AlarmPingSenderConfig(
    val isMqttPingWakeUp: Boolean = true,
    val isMqttAllowWhileIdle: Boolean = true,
    val shouldLimitBackgroundPings: Boolean = false,
    val pingWakeLockTimeout: Int = DEFAULT_PING_WAKELOCK_TIMEOUT_IN_SECONDS,
    val maxNumberOfBackgroundAlarmPingsAllowed: Int = MAX_NUMBER_OF_BACKGROUND_ALARM_PINGS,
    val useElapsedRealTimeAlarm: Boolean = false
)

private const val MAX_NUMBER_OF_BACKGROUND_ALARM_PINGS = 4
private const val DEFAULT_PING_WAKELOCK_TIMEOUT_IN_SECONDS = 0 // 0 seconds
