package com.gojek.mqtt.pingsender

import androidx.annotation.RestrictTo
import com.gojek.courier.logging.ILogger
import org.eclipse.paho.client.mqttv3.ILogger as PahoV3Logger
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttPingSender as PahoV3MqttPingSender
import org.eclipse.paho.client.mqttv3.internal.ClientComms

/**
 * Bridges Courier's paho-agnostic [MqttPingSender] onto the paho v3
 * [PahoV3MqttPingSender] contract. The paho v3 [ClientComms] is wrapped behind a
 * [PingSenderComms] facade so that the ping-sender logic stays free of paho types.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class PahoV3PingSenderAdapter(
    private val delegate: MqttPingSender,
    private val logger: ILogger
) : PahoV3MqttPingSender {

    override fun init(comms: ClientComms, pahoLogger: PahoV3Logger) {
        delegate.init(PahoV3PingSenderComms(comms, pahoLogger), logger)
    }

    override fun start() {
        delegate.start()
    }

    override fun stop() {
        delegate.stop()
    }

    override fun schedule(delayInMilliseconds: Long) {
        delegate.schedule(delayInMilliseconds)
    }
}

private class PahoV3PingSenderComms(
    private val comms: ClientComms,
    private val pahoLogger: PahoV3Logger
) : PingSenderComms {

    override val clientId: String?
        get() = comms.client?.clientId

    override val serverURI: String?
        get() = comms.client?.serverURI

    override val keepAliveMillis: Long
        get() = comms.keepAlive

    override fun checkActivity(forcePing: Boolean, callback: PingActionCallback): Boolean {
        val token = comms.checkForActivity(forcePing) ?: return false
        token.actionCallback = callback.toPahoV3ActionListener()
        return true
    }

    override fun sendPingRequest(callback: PingActionCallback): Boolean {
        val token = comms.sendPingRequest() ?: return false
        token.actionCallback = callback.toPahoV3ActionListener()
        return true
    }

    override fun setAppKillTime(time: Long) {
        pahoLogger.setAppKillTime(time)
    }
}

private fun PingActionCallback.toPahoV3ActionListener(): IMqttActionListener {
    val callback = this
    return object : IMqttActionListener {
        override fun onSuccess(asyncActionToken: IMqttToken) {
            callback.onSuccess()
        }

        override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
            callback.onFailure(exception)
        }
    }
}
