package com.gojek.mqtt.scheduler.runnable

import com.gojek.courier.QoS
import com.gojek.mqtt.client.IClientSchedulerBridge

internal class SubscribeRunnable(
    private val clientSchedulerBridge: IClientSchedulerBridge,
    private val topicMap: Map<String, QoS>
) : Runnable {
        override fun run() {
            clientSchedulerBridge.subscribeMqtt(topicMap)
        }
    }