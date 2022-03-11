package com.gojek.mqtt.auth

import com.gojek.mqtt.model.MqttConnectOptions

interface Authenticator {
    fun authenticate(
        connectOptions: MqttConnectOptions,
        forceRefresh: Boolean
    ): MqttConnectOptions
}