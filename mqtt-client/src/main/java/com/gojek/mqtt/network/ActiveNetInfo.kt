package com.gojek.mqtt.network

data class ActiveNetInfo(
    val connected: Boolean,
    val validated: Boolean,
    val networkType: Short
)
