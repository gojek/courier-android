package com.gojek.mqtt.model

data class ServerUri(val host: String, val port: Int, val scheme: String = "ssl") {
    override fun toString(): String {
        return "$scheme://$host:$port"
    }
}