package com.gojek.mqtt.scheduler.runnable

import com.gojek.mqtt.client.IClientSchedulerBridge

internal class ConnectionCheckRunnable(
    private val clientSchedulerBridge: IClientSchedulerBridge
) : Runnable {
    private var sleepTime: Long = 0
    fun setSleepTime(t: Long) {
        sleepTime = t
    }

    override fun run() {
        if (sleepTime > 0) {
            try {
                Thread.sleep(sleepTime)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            sleepTime = 0
        }
        clientSchedulerBridge.connectMqtt()
    }
}
