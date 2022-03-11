package com.gojek.mqtt.scheduler.runnable

import com.gojek.mqtt.client.IClientSchedulerBridge

internal class ResetParamsRunnable(
    private val clientSchedulerBridge: IClientSchedulerBridge
) : Runnable {
        override fun run() {
            clientSchedulerBridge.resetParams()
        }
    }