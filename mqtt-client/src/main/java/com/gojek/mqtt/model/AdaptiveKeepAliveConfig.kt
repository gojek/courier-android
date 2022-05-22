package com.gojek.mqtt.model

import com.gojek.mqtt.pingsender.AdaptiveMqttPingSender

data class AdaptiveKeepAliveConfig(
    val lowerBoundMinutes: Int,
    val upperBoundMinutes: Int,
    val stepMinutes: Int,
    val optimalKeepAliveResetLimit: Int,
    val pingSender: AdaptiveMqttPingSender
)
