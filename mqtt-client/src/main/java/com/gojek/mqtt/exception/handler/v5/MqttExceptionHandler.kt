package com.gojek.mqtt.exception.handler.v5

import org.eclipse.paho.mqttv5.common.MqttException

internal interface MqttExceptionHandler {
    fun handleException(mqttException: MqttException, reconnect: Boolean)
}
