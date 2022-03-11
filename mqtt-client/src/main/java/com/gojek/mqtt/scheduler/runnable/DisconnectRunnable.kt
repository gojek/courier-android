package com.gojek.mqtt.scheduler.runnable

import android.os.Handler
import com.gojek.mqtt.constants.MQTT_WAIT_BEFORE_RECONNECT_TIME_MS
import com.gojek.mqtt.client.IClientSchedulerBridge

internal class DisconnectRunnable(
    private val clientSchedulerBridge: IClientSchedulerBridge,
    private val mqttThreadHandler: Handler,
    private val connectionCheckRunnable: ConnectionCheckRunnable,
    private val shouldRemoveConnCheckRunnable: Boolean
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
                clientSchedulerBridge.connect(MQTT_WAIT_BEFORE_RECONNECT_TIME_MS) // try reconnection after 10 ms
            } else {
                try {
                    if (shouldRemoveConnCheckRunnable) {
                        // if you dont want to reconnect simply remove all connection check runnables
                        mqttThreadHandler.removeCallbacks(connectionCheckRunnable)
                    }
                } catch (e: Exception) {
                    //ignore
                }
            }
        }
        reconnect = true // resetting value after run
    }
}