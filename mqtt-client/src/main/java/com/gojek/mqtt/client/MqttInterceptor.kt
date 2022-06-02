package com.gojek.mqtt.client

interface MqttInterceptor {
    fun onMqttWireMessageSent(mqttWireMessageBytes: ByteArray)

    fun onMqttWireMessageReceived(mqttWireMessageBytes: ByteArray)
}

private class MqttInterceptorInternal(
    private val mqttInterceptor: MqttInterceptor
) : org.eclipse.paho.client.mqttv3.MqttInterceptor {
    override fun onMqttWireMessageSent(mqttWireMessageBytes: ByteArray) {
        mqttInterceptor.onMqttWireMessageSent(mqttWireMessageBytes)
    }

    override fun onMqttWireMessageReceived(mqttWireMessageBytes: ByteArray) {
        mqttInterceptor.onMqttWireMessageReceived(mqttWireMessageBytes)
    }
}

internal fun mapToPahoInterceptor(
    mqttInterceptor: MqttInterceptor
): org.eclipse.paho.client.mqttv3.MqttInterceptor {
    return MqttInterceptorInternal(mqttInterceptor)
}
