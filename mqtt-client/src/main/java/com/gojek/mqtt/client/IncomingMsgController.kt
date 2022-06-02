package com.gojek.mqtt.client

import com.gojek.mqtt.client.listener.MessageListener

internal interface IncomingMsgController {
    fun triggerHandleMessage()
    fun registerListener(topic: String, listener: MessageListener)
    fun unregisterListener(topic: String, listener: MessageListener)
}
