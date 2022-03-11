package com.gojek.mqtt.client.factory

import android.content.Context
import com.gojek.mqtt.client.MqttCourierClient
import com.gojek.mqtt.client.MqttClient
import com.gojek.mqtt.client.config.v3.MqttV3Configuration
import com.gojek.mqtt.client.internal.MqttClientInternal

class MqttClientFactory private constructor() {
    companion object {
        fun create(
            context: Context,
            mqttConfiguration: MqttV3Configuration
        ): MqttClient {
            val mqttClient = MqttClientInternal(
                context,
                mqttConfiguration
            )
            return MqttCourierClient(mqttClient)
        }
    }
}