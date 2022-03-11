package com.gojek.chuckmqtt.external

data class MqttChuckConfig(
    val retentionPeriod: Period = Period.ONE_HOUR,
    val showNotification: Boolean = true
)