package com.gojek.mqtt.scheduler.runnable

import com.gojek.courier.logging.ILogger
import com.gojek.mqtt.client.IClientSchedulerBridge

internal class ActivityCheckRunnable(
    private val clientSchedulerBridge: IClientSchedulerBridge,
    private val logger: ILogger
) : Runnable {
    override fun run() {
        if (clientSchedulerBridge.isConnected() || clientSchedulerBridge.isConnecting()) {
            try {
                logger.d(TAG, "Checking activity...")
                clientSchedulerBridge.checkActivity()
                clientSchedulerBridge.scheduleNextActivityCheck()
            } catch (e: Exception) {
                logger.e(TAG, "Exception in ActivityCheckRunnable", e)
            }
        }
    }

    companion object {
        const val TAG = "ActivityCheckRunnable"
    }
}