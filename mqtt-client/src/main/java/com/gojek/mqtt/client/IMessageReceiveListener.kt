package com.gojek.mqtt.client

internal interface IMessageReceiveListener {
    fun messageArrived(topic: String, byteArray: ByteArray): Boolean
}
