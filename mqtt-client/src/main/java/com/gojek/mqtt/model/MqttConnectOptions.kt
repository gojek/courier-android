package com.gojek.mqtt.model

import com.gojek.mqtt.model.MqttVersion.VERSION_3_1_1

data class MqttConnectOptions(
    val serverUris: List<ServerUri>,
    val keepAlive: KeepAlive,
    val clientId: String,
    val username: String,
    val password: String,
    val isCleanSession: Boolean,
    val readTimeoutSecs: Int = -1,
    val version: MqttVersion = VERSION_3_1_1
)

enum class MqttVersion(internal val protocolName: String, internal val protocolLevel: Int) {
    VERSION_3_1("MQIsdp", 3), VERSION_3_1_1("MQTT", 4)
}