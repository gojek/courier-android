package com.gojek.timer.pingsender

import androidx.annotation.VisibleForTesting
import com.gojek.courier.extensions.fromMillisToSeconds
import com.gojek.courier.extensions.fromNanosToMillis
import com.gojek.courier.logging.ILogger
import com.gojek.courier.utils.Clock
import com.gojek.mqtt.pingsender.IPingSenderEvents
import com.gojek.mqtt.pingsender.MqttPingSender
import com.gojek.mqtt.pingsender.NoOpPingSenderEvents
import com.gojek.mqtt.pingsender.PingActionCallback
import com.gojek.mqtt.pingsender.PingSenderComms
import java.util.Timer
import java.util.TimerTask

/**
 * Default ping sender implementation
 *
 * This class implements the [MqttPingSender] pinger interface allowing applications to send ping packet to server every keep alive interval.
 *
 * @see MqttPingSender
 */
internal class TimerPingSender(
    private val pingSenderConfig: TimerPingSenderConfig,
    private val clock: Clock = Clock(),
    private val timerFactory: TimerFactory = TimerFactory()
) : MqttPingSender {
    private lateinit var comms: PingSenderComms
    private lateinit var logger: ILogger

    @VisibleForTesting
    internal lateinit var timer: Timer

    private var pingSenderEvents: IPingSenderEvents = NoOpPingSenderEvents()

    override fun init(
        comms: PingSenderComms,
        logger: ILogger
    ) {
        this.comms = comms
        this.logger = logger
    }

    override fun start() {
        logger.d(TAG, "Starting timer")
        val clientId = comms.clientId

        timer = timerFactory.getTimer("MQTT Ping: $clientId")
        schedule(comms.keepAliveMillis)
    }

    override fun stop() {
        logger.d(TAG, "Stopping timer")
        if (::timer.isInitialized) {
            timer.cancel()
        }
    }

    override fun schedule(delayInMilliseconds: Long) {
        timer.schedule(PingTask(), delayInMilliseconds)
        pingSenderEvents.mqttPingScheduled(delayInMilliseconds.fromMillisToSeconds(), comms.keepAliveMillis.fromMillisToSeconds())
    }

    override fun setPingEventHandler(pingSenderEvents: IPingSenderEvents) {
        this.pingSenderEvents = pingSenderEvents
    }

    internal inner class PingTask : TimerTask() {
        override fun run() {
            logger.d(TAG, "Sending ping")
            val serverUri = comms.serverURI ?: ""
            val keepAliveMillis = comms.keepAliveMillis
            pingSenderEvents.mqttPingInitiated(serverUri, keepAliveMillis.fromMillisToSeconds())
            val sTime = clock.nanoTime()
            val initiated = comms.checkActivity(
                pingSenderConfig.sendForcePing,
                object : PingActionCallback {
                    override fun onSuccess() {
                        logger.d(TAG, "Mqtt Ping Sent successfully")
                        val timeTaken = (clock.nanoTime() - sTime).fromNanosToMillis()
                        pingSenderEvents.pingEventSuccess(serverUri, timeTaken, keepAliveMillis.fromMillisToSeconds())
                    }

                    override fun onFailure(exception: Throwable) {
                        logger.d(TAG, "Mqtt Ping Sent failed")
                        val timeTaken = (clock.nanoTime() - sTime).fromNanosToMillis()
                        pingSenderEvents.pingEventFailure(
                            serverUri,
                            timeTaken,
                            exception,
                            keepAliveMillis.fromMillisToSeconds()
                        )
                    }
                }
            )
            if (!initiated) {
                logger.d(TAG, "Mqtt Ping Token null")
                pingSenderEvents.pingMqttTokenNull(serverUri, keepAliveMillis.fromMillisToSeconds())
                return
            }
        }
    }
}

private const val TAG = "TimerPingSender"
