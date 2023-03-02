package com.gojek.mqtt.exception.handler

import org.eclipse.paho.client.mqtt.MqttException

internal interface MqttExceptionHandler {
    fun handleException(mqttException: MqttException, reconnect: Boolean)
}
