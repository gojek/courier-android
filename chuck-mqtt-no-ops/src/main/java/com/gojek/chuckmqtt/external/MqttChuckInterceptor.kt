package com.gojek.chuckmqtt.external

import android.content.Context
import com.gojek.mqtt.client.MqttInterceptor

class MqttChuckInterceptor(
    context: Context,
    mqttChuckConfig: MqttChuckConfig
) : MqttInterceptor {

    override fun onMqttWireMessageReceived(mqttWireMessageBytes: ByteArray) {
        // no ops
    }

    override fun onMqttWireMessageSent(mqttWireMessageBytes: ByteArray) {
        // no ops
    }
}