package com.gojek.mqtt.scheduler.runnable

import com.gojek.mqtt.client.IClientSchedulerBridge

internal class MqttExceptionRunnable(
    private val clientSchedulerBridge: IClientSchedulerBridge
) : Runnable {
    var exception: Exception? = null
    var reconnect = false
    override fun run() {
        clientSchedulerBridge.handleMqttException(exception, reconnect)
        exception = null
        reconnect = true
    }

    fun setParameters(e: Exception?, reconnect: Boolean) {
        exception = e
        this.reconnect = reconnect
    }
}
