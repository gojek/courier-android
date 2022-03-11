package com.gojek.mqtt.scheduler.runnable

import com.gojek.mqtt.client.IClientSchedulerBridge

internal class UnsubscribeRunnable(
    private val clientSchedulerBridge: IClientSchedulerBridge,
    private val topics: Set<String>
) : Runnable {
        override fun run() {
            clientSchedulerBridge.unsubscribeMqtt(topics)
        }
    }