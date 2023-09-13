package com.gojek.workmanager.pingsender

data class WorkManagerPingSenderConfig(
    val timeoutSeconds: Long = DEFAULT_PING_TIMEOUT_SECS,
    val sendForcePing: Boolean = false
)

internal const val DEFAULT_PING_TIMEOUT_SECS = 30L
