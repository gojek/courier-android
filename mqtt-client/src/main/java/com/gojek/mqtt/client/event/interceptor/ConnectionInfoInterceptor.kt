package com.gojek.mqtt.client.event.interceptor

import com.gojek.mqtt.client.connectioninfo.ConnectionInfoStore
import com.gojek.mqtt.event.MqttEvent

internal class ConnectionInfoInterceptor(
    private val connectionInfoStore: ConnectionInfoStore
) : EventInterceptor {
    override fun intercept(mqttEvent: MqttEvent): MqttEvent {
        mqttEvent.connectionInfo = connectionInfoStore.getConnectionInfo()
        return mqttEvent
    }
}
