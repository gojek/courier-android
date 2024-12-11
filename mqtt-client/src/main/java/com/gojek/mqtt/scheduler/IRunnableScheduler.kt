package com.gojek.mqtt.scheduler

import com.gojek.courier.QoS
import com.gojek.mqtt.client.model.MqttSendPacket

internal interface IRunnableScheduler {
    fun connectMqtt()

    fun connectMqtt(timeMillis: Long)

    fun disconnectMqtt(reconnect: Boolean, clearState: Boolean = false)

    fun scheduleNextActivityCheck()

    fun scheduleMqttHandleExceptionRunnable(e: Exception?, reconnect: Boolean)

    fun scheduleNextConnectionCheck()

    fun scheduleNextConnectionCheck(reconnectTimeSecs: Long)

    fun scheduleSubscribe(delayMillis: Long, topicMap: Map<String, QoS>)

    fun scheduleUnsubscribe(delayMillis: Long, topics: Set<String>)

    fun scheduleResetParams(delayMillis: Long)

    fun scheduleAuthFailureRunnable(delayMillis: Long)

    fun sendMessage(mqttSendPacket: MqttSendPacket): Boolean

    fun start()
    fun stop()
}
