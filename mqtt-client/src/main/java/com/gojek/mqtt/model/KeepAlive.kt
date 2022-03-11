package com.gojek.mqtt.model

data class KeepAlive(
    val timeSeconds: Int,
    internal val isOptimal: Boolean = false
)