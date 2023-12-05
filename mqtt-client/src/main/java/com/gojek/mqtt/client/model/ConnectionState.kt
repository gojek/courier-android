package com.gojek.mqtt.client.model

import com.gojek.mqtt.client.MqttClient

enum class ConnectionState {
    /**
     * Represents state when [MqttClient] is not created/initialised
     */
    UNINITIALISED,

    /**
     * Represents state when [MqttClient] is created/initialised
     */
    INITIALISED,

    /**
     * Represents state when [MqttClient] is connecting to a remote broker
     */
    CONNECTING,

    /**
     * Represents state when [MqttClient] is connected to a remote broker
     */
    CONNECTED,

    /**
     * Represents state when [MqttClient] is disconnecting the current MQTT connection
     */
    DISCONNECTING,

    /**
     * Represents state when [MqttClient] is not connected to any remote broker
     */
    DISCONNECTED
}
