package com.gojek.mqtt.pingsender

import androidx.annotation.RestrictTo
import com.gojek.courier.logging.ILogger
import org.eclipse.paho.mqttv5.client.IMqttToken
import org.eclipse.paho.mqttv5.client.MqttActionListener
import org.eclipse.paho.mqttv5.client.MqttPingSender as PahoV5MqttPingSender
import org.eclipse.paho.mqttv5.client.internal.ClientComms

/**
 * Bridges Courier's paho-agnostic [MqttPingSender] onto the paho v5
 * [PahoV5MqttPingSender] contract. The paho v5 [ClientComms] is wrapped behind a
 * [PingSenderComms] facade so that the ping-sender logic stays free of paho types.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class PahoV5PingSenderAdapter(
    private val delegate: MqttPingSender,
    private val logger: ILogger
) : PahoV5MqttPingSender {

    override fun init(comms: ClientComms) {
        delegate.init(PahoV5PingSenderComms(comms), logger)
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

private class PahoV5PingSenderComms(
    private val comms: ClientComms
) : PingSenderComms {

    override val clientId: String?
        get() = comms.client?.clientId

    override val serverURI: String?
        get() = comms.client?.serverURI

    override val keepAliveMillis: Long
        get() = comms.keepAlive

    override fun checkActivity(forcePing: Boolean, callback: PingActionCallback): Boolean {
        // Paho v5 does not support a force-ping variant; the keep-alive check
        // initiates a ping when required.
        return comms.checkForActivity(callback.toPahoV5ActionListener()) != null
    }

    override fun sendPingRequest(callback: PingActionCallback): Boolean {
        return comms.checkForActivity(callback.toPahoV5ActionListener()) != null
    }

    override fun setAppKillTime(time: Long) {
        comms.logger?.setAppKillTime(time)
    }
}

private fun PingActionCallback.toPahoV5ActionListener(): MqttActionListener {
    val callback = this
    return object : MqttActionListener {
        override fun onSuccess(asyncActionToken: IMqttToken) {
            callback.onSuccess()
        }

        override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
            callback.onFailure(exception)
        }
    }
}
