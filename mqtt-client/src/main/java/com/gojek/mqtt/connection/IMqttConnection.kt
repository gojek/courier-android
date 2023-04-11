package com.gojek.mqtt.connection

import com.gojek.courier.QoS
import com.gojek.mqtt.client.IMessageReceiveListener
import com.gojek.mqtt.client.model.MqttSendPacket
import com.gojek.mqtt.model.MqttConnectOptions
import com.gojek.mqtt.policies.hostfallback.IHostFallbackPolicy

internal interface IMqttConnection {
    fun connect(
        mqttConnectOptions: MqttConnectOptions,
        messageReceiveListener: IMessageReceiveListener,
        hostFallbackPolicy: IHostFallbackPolicy,
        subscriptionTopicMap: Map<String, QoS>
    )

    fun subscribe(topicMap: Map<String, QoS>)
    fun unsubscribe(topics: Set<String>)

    fun publish(mqttPacket: MqttSendPacket)

    fun disconnect()

    fun handleException(exception: Exception?, reconnect: Boolean)
    fun isConnected(): Boolean
    fun isConnecting(): Boolean
    fun isDisconnecting(): Boolean
    fun isDisconnected(): Boolean
    fun isForceDisconnect(): Boolean
    fun getServerURI(): String?
    fun checkActivity()
    fun resetParams()
    fun shutDown()
}
