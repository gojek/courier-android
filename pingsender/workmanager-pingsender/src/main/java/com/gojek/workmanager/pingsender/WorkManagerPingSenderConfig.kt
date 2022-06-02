package com.gojek.workmanager.pingsender

data class WorkManagerPingSenderConfig(
    val timeoutSeconds: Long = DEFAULT_PING_TIMEOUT_SECS
)

internal const val DEFAULT_PING_TIMEOUT_SECS = 30L
