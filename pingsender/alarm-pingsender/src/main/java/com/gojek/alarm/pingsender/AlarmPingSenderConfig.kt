package com.gojek.alarm.pingsender

data class AlarmPingSenderConfig(
    val isMqttPingWakeUp: Boolean = true,
    val isMqttAllowWhileIdle: Boolean = true,
    val pingWakeLockTimeout: Int = DEFAULT_PING_WAKELOCK_TIMEOUT_IN_SECONDS,
    val useElapsedRealTimeAlarm: Boolean = false,
    val sendForcePing: Boolean = false
)

private const val DEFAULT_PING_WAKELOCK_TIMEOUT_IN_SECONDS = 0 // 0 seconds
