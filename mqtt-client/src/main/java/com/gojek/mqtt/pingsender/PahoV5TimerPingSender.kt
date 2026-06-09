package com.gojek.mqtt.pingsender

import com.gojek.courier.extensions.fromMillisToSeconds
import com.gojek.courier.extensions.fromNanosToMillis
import com.gojek.courier.logging.ILogger
import com.gojek.courier.utils.Clock
import java.util.Timer
import java.util.TimerTask
import org.eclipse.paho.mqttv5.client.IMqttToken
import org.eclipse.paho.mqttv5.client.MqttActionListener
import org.eclipse.paho.mqttv5.client.MqttPingSender
import org.eclipse.paho.mqttv5.client.internal.ClientComms

/**
 * Timer based ping sender for the MQTT v5 client. This mirrors the MQTT v3
 * {@code TimerPingSender} behaviour but operates against the v5 paho
 * {@link ClientComms}. It is used as the v5 bridge for the Courier ping-sender
 * abstraction (which is itself bound to the v3 paho types).
 */
internal class PahoV5TimerPingSender(
    private val logger: ILogger,
    private val pingSenderEvents: IPingSenderEvents = NoOpPingSenderEvents(),
    private val clock: Clock = Clock()
) : MqttPingSender {
    private lateinit var comms: ClientComms
    private var timer: Timer? = null

    override fun init(comms: ClientComms) {
        this.comms = comms
    }

    override fun start() {
        val clientId = comms.client.clientId
        logger.d(TAG, "Starting v5 ping timer for $clientId")
        timer = Timer("MQTT Ping: $clientId")
        schedule(comms.keepAlive)
    }

    override fun stop() {
        logger.d(TAG, "Stopping v5 ping timer")
        timer?.cancel()
        timer = null
    }

    override fun schedule(delayInMilliseconds: Long) {
        timer?.schedule(PingTask(), delayInMilliseconds)
        pingSenderEvents.mqttPingScheduled(
            delayInMilliseconds.fromMillisToSeconds(),
            comms.keepAlive.fromMillisToSeconds()
        )
    }

    private inner class PingTask : TimerTask() {
        override fun run() {
            val serverUri = comms.client?.serverURI ?: ""
            val keepAliveMillis = comms.keepAlive
            logger.d(TAG, "Sending v5 ping")
            pingSenderEvents.mqttPingInitiated(serverUri, keepAliveMillis.fromMillisToSeconds())
            val sTime = clock.nanoTime()
            val token = comms.checkForActivity(object : MqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken) {
                    val timeTaken = (clock.nanoTime() - sTime).fromNanosToMillis()
                    pingSenderEvents.pingEventSuccess(
                        serverUri,
                        timeTaken,
                        keepAliveMillis.fromMillisToSeconds()
                    )
                }

                override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                    val timeTaken = (clock.nanoTime() - sTime).fromNanosToMillis()
                    pingSenderEvents.pingEventFailure(
                        serverUri,
                        timeTaken,
                        exception,
                        keepAliveMillis.fromMillisToSeconds()
                    )
                }
            })
            if (token == null) {
                pingSenderEvents.pingMqttTokenNull(serverUri, keepAliveMillis.fromMillisToSeconds())
            }
        }
    }

    companion object {
        private const val TAG = "PahoV5TimerPingSender"
    }
}
