package com.gojek.mqtt.client

import com.gojek.courier.Message
import com.gojek.courier.QoS
import com.gojek.mqtt.client.internal.MqttClientInternal
import com.gojek.mqtt.client.listener.MessageListener
import com.gojek.mqtt.client.model.ConnectionState
import com.gojek.mqtt.model.MqttConnectOptions
import com.gojek.mqtt.model.MqttPacket

internal class MqttCourierClient(
    private val mqttClient: MqttClientInternal
): MqttClient {
    override fun connect(connectOptions: MqttConnectOptions) {
        mqttClient.connect(connectOptions)
    }

    override fun getCurrentState(): ConnectionState {
        return mqttClient.getCurrentState()
    }

    override fun disconnect(clearState: Boolean) {
        mqttClient.disconnect(clearState)
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

    override fun send(message: Message, topic: String, qos: QoS): Boolean {
        return mqttClient.send(MqttPacket((message as Message.Bytes).value, topic, qos))
    }

    override fun receive(listener: MessageListener) {
        mqttClient.receive(listener)
    }
}