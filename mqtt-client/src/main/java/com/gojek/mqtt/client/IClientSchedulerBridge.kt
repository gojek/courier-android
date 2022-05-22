package com.gojek.mqtt.client

import com.gojek.courier.QoS
import com.gojek.mqtt.client.model.MqttSendPacket

internal interface IClientSchedulerBridge {
    fun connectMqtt()
    fun connect(timeMillis: Long)
    fun sendMessage(mqttPacket: MqttSendPacket)
    fun disconnectMqtt(clearState: Boolean)
    fun handleMqttException(exception: Exception?, reconnect: Boolean)
    fun isConnected(): Boolean
    fun isConnecting(): Boolean
    fun checkActivity()
    fun scheduleNextActivityCheck()
    fun subscribeMqtt(topicMap: Map<String, QoS>)
    fun unsubscribeMqtt(topics: Set<String>)
    fun resetParams()
    fun handleAuthFailure()
}
