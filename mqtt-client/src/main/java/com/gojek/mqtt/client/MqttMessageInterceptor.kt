package com.gojek.mqtt.client

interface MqttMessageInterceptor {
    fun intercept(mqttWireMessageBytes: ByteArray, isSent: Boolean): ByteArray
}

private class MqttMessageInterceptorInternal(
    private val mqttInterceptor: MqttMessageInterceptor
) : org.eclipse.paho.client.mqttv3.MqttMessageInterceptor {
    override fun intercept(mqttWireMessageBytes: ByteArray, isSent: Boolean): ByteArray {
        return mqttInterceptor.intercept(mqttWireMessageBytes, isSent)
    }
}

internal fun mapToPahoMessageInterceptor(
    mqttInterceptor: MqttMessageInterceptor
): org.eclipse.paho.client.mqttv3.MqttMessageInterceptor {
    return MqttMessageInterceptorInternal(mqttInterceptor)
}
