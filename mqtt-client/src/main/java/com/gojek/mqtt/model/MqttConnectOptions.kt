package com.gojek.mqtt.model

data class MqttConnectOptions(
    val serverUris: List<ServerUri>,
    val keepAlive: KeepAlive,
    val clientId: String,
    val username: String,
    val password: String,
    val isCleanSession: Boolean,
    val readTimeoutSecs: Int = -1
)