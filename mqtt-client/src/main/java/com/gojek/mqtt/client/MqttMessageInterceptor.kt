package com.gojek.mqtt.client

interface MqttMessageInterceptor {
    fun intercept(topic: String, mqttWireMessageBytes: ByteArray, isSent: Boolean): ByteArray
}

private class MqttMessageInterceptorInternal(
    private val mqttInterceptor: MqttMessageInterceptor
) : org.eclipse.paho.client.mqttv3.MqttMessageInterceptor {
    override fun intercept(topic: String, mqttWireMessageBytes: ByteArray, isSent: Boolean): ByteArray {
        return mqttInterceptor.intercept(topic, mqttWireMessageBytes, isSent)
    }
}

internal fun mapToPahoMessageInterceptor(
    mqttInterceptor: MqttMessageInterceptor
): org.eclipse.paho.client.mqttv3.MqttMessageInterceptor {
    return MqttMessageInterceptorInternal(mqttInterceptor)
}
