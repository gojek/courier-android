package com.gojek.mqtt.scheduler.runnable

import com.gojek.mqtt.client.IClientSchedulerBridge

internal class AuthFailureRunnable(
    private val clientSchedulerBridge: IClientSchedulerBridge
) : Runnable {
    override fun run() {
        clientSchedulerBridge.handleAuthFailure()
    }
}
