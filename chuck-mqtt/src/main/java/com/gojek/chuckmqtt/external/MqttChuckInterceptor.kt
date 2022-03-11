package com.gojek.chuckmqtt.external

import android.content.Context
import com.gojek.chuckmqtt.internal.MqttChuck
import com.gojek.mqtt.client.MqttInterceptor

class MqttChuckInterceptor(
    context: Context,
    mqttChuckConfig: MqttChuckConfig
): MqttInterceptor {

    private val collector by lazy { MqttChuck.collector() }

    init {
        MqttChuck.initialise(context, mqttChuckConfig)
    }

    override fun onMqttWireMessageReceived(mqttWireMessageBytes: ByteArray) {
        collector.onMessageReceived(mqttWireMessageBytes)
    }

    override fun onMqttWireMessageSent(mqttWireMessageBytes: ByteArray) {
        collector.onMessageSent(mqttWireMessageBytes)
    }
}