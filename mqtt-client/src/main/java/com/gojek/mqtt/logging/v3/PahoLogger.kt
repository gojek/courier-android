package com.gojek.mqtt.logging.v3

import org.eclipse.paho.client.mqtt.ILogger
import org.eclipse.paho.client.mqttv3.internal.wire.MqttWireMessage

internal class PahoLogger(
    private val logger: com.gojek.courier.logging.ILogger
) : ILogger {
    override fun wtf(tag: String, msg: String, tr: Throwable) {
    }

    override fun wtf(tag: String, msg: String) {
    }

    override fun logEvent(
        type: String,
        isSuccessful: Boolean,
        endPoint: String,
        timeTaken: Long,
        throwable: Throwable?,
        errorCode: Int,
        timestamp: Long,
        packetSize: Long,
        threadId: String,
        uniqueMsgId: Int
    ) {
    }

    override fun w(tag: String, msg: String) {
        logger.w(tag, msg)
    }

    override fun w(tag: String, msg: String, tr: Throwable) {
        logger.w(tag, msg)
    }

    override fun w(tag: String, tr: Throwable) {
        logger.w(tag, tr)
    }

    override fun v(tag: String, msg: String) {
        logger.v(tag, msg)
    }

    override fun v(tag: String, msg: String, tr: Throwable) {
        logger.v(tag, msg, tr)
    }

    override fun logMessageReceivedData(message: MqttWireMessage?) {
    }

    override fun logMessageSentData(message: MqttWireMessage?) {
    }

    override fun setAppKillTime(time: Long) {
    }

    override fun i(tag: String, msg: String) {
        logger.i(tag, msg)
    }

    override fun i(tag: String, msg: String, sendLogMsg: String) {
        logger.i(tag, msg)
    }

    override fun i(tag: String, msg: String, tr: Throwable) {
        logger.i(tag, msg, tr)
    }

    override fun e(tag: String, msg: String) {
        logger.e(tag, msg)
    }

    override fun e(tag: String, msg: String, tr: Throwable) {
        logger.e(tag, msg, tr)
    }

    override fun d(tag: String, msg: String) {
        logger.d(tag, msg)
    }

    override fun d(tag: String, msg: String, sendLogMsg: String) {
        logger.d(tag, msg)
    }

    override fun d(tag: String, msg: String, tr: Throwable) {
        logger.d(tag, msg, tr)
    }

    override fun logFastReconnectEvent(
        fastReconnectCheckStartTime: Long,
        lastInboundActivity: Long
    ) {
    }

    override fun logInitEvent(eventType: String, ts: Long, endPoint: String) {
    }

    override fun logMqttThreadEvent(eventType: String, timeTaken: Long, threadId: String) {
    }
}
