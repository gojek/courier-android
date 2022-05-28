package com.gojek.mqtt.scheduler.runnable

import com.gojek.mqtt.client.IClientSchedulerBridge
import com.gojek.mqtt.constants.MQTT_WAIT_BEFORE_RECONNECT_TIME_MS

internal class DisconnectRunnable(
    private val clientSchedulerBridge: IClientSchedulerBridge
) : Runnable {
    private var reconnect = true
    private var clearState = false

    fun setReconnect(isReconnect: Boolean) {
        reconnect = isReconnect
    }

    fun setClearState(clearState: Boolean) {
        this.clearState = clearState
    }

    override fun run() {
        try {
            clientSchedulerBridge.disconnectMqtt(clearState)
        } finally {
            if (reconnect) {
                // try reconnection after 10 ms
                clientSchedulerBridge.connect(MQTT_WAIT_BEFORE_RECONNECT_TIME_MS)
            }
        }
        reconnect = true // resetting value after run
    }
}
