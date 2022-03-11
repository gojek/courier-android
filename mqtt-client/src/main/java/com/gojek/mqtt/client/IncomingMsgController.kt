package com.gojek.mqtt.client

internal interface IncomingMsgController {
    fun triggerHandleMessage()
}