package com.gojek.mqtt.handler

import android.os.Handler
import android.os.Looper
import android.os.Message
import com.gojek.courier.logging.ILogger
import com.gojek.mqtt.constants.MESSAGE
import com.gojek.mqtt.constants.MSG_APP_PUBLISH
import com.gojek.mqtt.client.IClientSchedulerBridge
import com.gojek.mqtt.client.model.MqttSendPacket

internal class IncomingHandler(
    looper: Looper,
    private val clientSchedulerBridge: IClientSchedulerBridge,
    private val logger: ILogger
) : Handler(looper) {
    override fun handleMessage(msg: Message) {
        try {
            if (msg.what == MSG_APP_PUBLISH) {
                val bundle = msg.data
                val packet: MqttSendPacket = bundle.getParcelable(MESSAGE)!!
                clientSchedulerBridge.sendMessage(packet)
            }
        } catch (e: Exception) {
            logger.e("IncomingHandler", "Exception", e)
        }
    }
}