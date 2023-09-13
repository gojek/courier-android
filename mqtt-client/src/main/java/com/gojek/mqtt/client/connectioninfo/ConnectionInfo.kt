package com.gojek.mqtt.client.connectioninfo

data class ConnectionInfo(
    val clientId: String,
    val username: String,
    val keepaliveSeconds: Int,
    val connectTimeout: Int,
    val host: String,
    val port: Int,
    val scheme: String,
    val cleanSession: Boolean
)
