package com.gojek.mqtt.client.v3

import com.gojek.courier.QoS
import com.gojek.keepalive.KeepAliveFailureHandler
import com.gojek.mqtt.client.internal.KeepAliveProvider
import com.gojek.mqtt.client.listener.MessageListener
import com.gojek.mqtt.client.model.ConnectionState
import com.gojek.mqtt.constants.MQTT_WAIT_BEFORE_RECONNECT_TIME_MS
import com.gojek.mqtt.model.MqttConnectOptions
import com.gojek.mqtt.model.MqttPacket

internal interface IAndroidMqttClient {
    fun connect(connectOptions: MqttConnectOptions)
    fun connect(timeMillis: Long = MQTT_WAIT_BEFORE_RECONNECT_TIME_MS)
    fun reconnect()
    fun disconnect(clearState: Boolean = false)
    fun send(mqttPacket: MqttPacket): Boolean
    fun receive(listener: MessageListener)
    fun isConnected(): Boolean
    fun subscribe(topicMap: Map<String, QoS>)
    fun unsubscribe(topics: List<String>)
    fun getCurrentState(): ConnectionState
}