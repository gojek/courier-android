package com.gojek.mqtt.scheduler.runnable

import com.gojek.mqtt.client.IClientSchedulerBridge
import com.gojek.mqtt.client.model.MqttSendPacket

internal class SendMessageRunnable(
    private val packet: MqttSendPacket,
    private val clientSchedulerBridge: IClientSchedulerBridge
) : Runnable {

    override fun run() {
        clientSchedulerBridge.sendMessage(packet)
    }
}
