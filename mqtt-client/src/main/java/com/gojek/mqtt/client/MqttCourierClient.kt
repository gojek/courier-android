package com.gojek.mqtt.client

import com.gojek.courier.Message
import com.gojek.courier.QoS
import com.gojek.courier.callback.SendMessageCallback
import com.gojek.mqtt.client.internal.MqttClientInternal
import com.gojek.mqtt.client.listener.MessageListener
import com.gojek.mqtt.client.model.ConnectionState
import com.gojek.mqtt.event.EventHandler
import com.gojek.mqtt.model.MqttConnectOptions
import com.gojek.mqtt.model.MqttPacket

internal class MqttCourierClient(
    private val mqttClient: MqttClientInternal
) : MqttClient {
    override fun connect(connectOptions: MqttConnectOptions) {
        mqttClient.connect(connectOptions)
    }

    override fun getCurrentState(): ConnectionState {
        return mqttClient.getCurrentState()
    }

    override fun disconnect(clearState: Boolean) {
        if (clearState) {
            mqttClient.destroy()
        } else {
            mqttClient.disconnect()
        }
    }

    override fun reconnect() {
        mqttClient.reconnect()
    }

    override fun subscribe(topic: Pair<String, QoS>, vararg topics: Pair<String, QoS>) {
        mqttClient.subscribe(topic, *topics)
    }

    override fun unsubscribe(topic: String, vararg topics: String) {
        mqttClient.unsubscribe(topic, *topics)
    }

    override fun send(message: Message, topic: String, qos: QoS, sendMessageCallback: SendMessageCallback): Boolean {
        return mqttClient.send(MqttPacket((message as Message.Bytes).value, topic, qos), sendMessageCallback)
    }

    override fun addMessageListener(topic: String, listener: MessageListener) {
        mqttClient.addMessageListener(topic, listener)
    }

    override fun removeMessageListener(topic: String, listener: MessageListener) {
        mqttClient.removeMessageListener(topic, listener)
    }

    override fun addGlobalMessageListener(listener: MessageListener) {
        mqttClient.addGlobalMessageListener(listener)
    }

    override fun addEventHandler(eventHandler: EventHandler) {
        mqttClient.addEventHandler(eventHandler)
    }

    override fun removeEventHandler(eventHandler: EventHandler) {
        mqttClient.removeEventHandler(eventHandler)
    }
}
