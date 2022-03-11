package com.gojek.mqtt.exception.handler.v3

import org.eclipse.paho.client.mqttv3.MqttException

internal interface MqttExceptionHandler {
    fun handleException(mqttException: MqttException, reconnect: Boolean)
}