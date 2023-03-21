package com.gojek.timer.pingsender

import androidx.annotation.VisibleForTesting
import com.gojek.courier.extensions.fromMillisToSeconds
import com.gojek.courier.extensions.fromNanosToMillis
import com.gojek.courier.utils.Clock
import com.gojek.mqtt.pingsender.IPingSenderEvents
import com.gojek.mqtt.pingsender.MqttPingSender
import com.gojek.mqtt.pingsender.NoOpPingSenderEvents
import java.util.Timer
import java.util.TimerTask
import org.eclipse.paho.client.mqtt.ILogger
import org.eclipse.paho.client.mqtt.internal.IClientComms
import org.eclipse.paho.client.mqtt.internal.PingActivityCallBack

/**
 * Default ping sender implementation
 *
 * This class implements the [MqttPingSender] pinger interface allowing applications to send ping packet to server every keep alive interval.
 *
 * @see MqttPingSender
 */
internal class TimerPingSender(
    private val clock: Clock = Clock(),
    private val timerFactory: TimerFactory = TimerFactory()
) : MqttPingSender {
    private lateinit var comms: IClientComms
    private lateinit var logger: ILogger

    @VisibleForTesting
    internal lateinit var timer: Timer

    private var pingSenderEvents: IPingSenderEvents = NoOpPingSenderEvents()

    override fun init(
        comms: IClientComms,
        logger: ILogger
    ) {
        this.logger = logger
    }

    override fun start() {
        logger.d(TAG, "Starting timer")
        val clientId = comms.clientId

        timer = timerFactory.getTimer("MQTT Ping: $clientId")
        schedule(comms.keepAlive)
    }

    override fun stop() {
        logger.d(TAG, "Stopping timer")
        if (::timer.isInitialized) {
            timer.cancel()
        }
    }

    override fun schedule(delayInMilliseconds: Long) {
        timer.schedule(PingTask(), delayInMilliseconds)
        pingSenderEvents.mqttPingScheduled(delayInMilliseconds.fromMillisToSeconds(), comms.keepAlive.fromMillisToSeconds())
    }

    override fun setPingEventHandler(pingSenderEvents: IPingSenderEvents) {
        this.pingSenderEvents = pingSenderEvents
    }

    internal inner class PingTask : TimerTask() {
        override fun run() {
            logger.d(TAG, "Sending ping")
            val serverUri = comms.serverUri ?: ""
            val keepAliveMillis = comms.keepAlive
            pingSenderEvents.mqttPingInitiated(comms.serverUri, keepAliveMillis.fromMillisToSeconds())
            val sTime = clock.nanoTime()
            comms.checkActivityWithCallback(object: PingActivityCallBack {
                override fun onPingMqttTokenNull() {
                    logger.d(TAG, "Mqtt Ping Token null")
                    pingSenderEvents.pingMqttTokenNull(serverUri, keepAliveMillis.fromMillisToSeconds())
                }

                override fun onSuccess() {
                    logger.d(TAG, "Mqtt Ping Sent successfully")
                    val timeTaken = (clock.nanoTime() - sTime).fromNanosToMillis()
                    pingSenderEvents.pingEventSuccess(serverUri, timeTaken, keepAliveMillis.fromMillisToSeconds())

                }

                override fun onFailure(throwable: Throwable) {
                    logger.d(TAG, "Mqtt Ping Sent failed")
                    val timeTaken = (clock.nanoTime() - sTime).fromNanosToMillis()
                    pingSenderEvents.pingEventFailure(
                        serverUri,
                        timeTaken,
                        throwable,
                        keepAliveMillis.fromMillisToSeconds()
                    )
                }
            })
        }
    }
}

private const val TAG = "TimerPingSender"
