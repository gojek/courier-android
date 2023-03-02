package com.gojek.mqtt.client

import com.gojek.courier.Message
import com.gojek.courier.QoS
import com.gojek.mqtt.client.listener.MessageListener
import com.gojek.mqtt.client.model.ConnectionState
import com.gojek.mqtt.event.EventHandler
import com.gojek.mqtt.model.MqttConnectOptions

interface MqttClient {
    fun connect(connectOptions: MqttConnectOptions)
    fun getCurrentState(): ConnectionState
    fun disconnect(clearState: Boolean = false)
    fun reconnect()
    fun subscribe(topic: Pair<String, QoS>, vararg topics: Pair<String, QoS>)
    fun unsubscribe(topic: String, vararg topics: String)
    fun send(message: Message, topic: String, qos: QoS): Boolean
    fun addMessageListener(topic: String, listener: MessageListener)
    fun removeMessageListener(topic: String, listener: MessageListener)
    fun addGlobalMessageListener(listener: MessageListener)
    fun addEventHandler(eventHandler: EventHandler)
    fun removeEventHandler(eventHandler: EventHandler)
}
