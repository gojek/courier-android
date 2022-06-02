package com.gojek.workmanager.pingsender

import androidx.annotation.VisibleForTesting
import com.gojek.courier.extensions.fromMillisToSeconds
import com.gojek.courier.extensions.fromNanosToMillis
import com.gojek.courier.utils.Clock
import com.gojek.mqtt.pingsender.AdaptiveMqttPingSender
import com.gojek.mqtt.pingsender.IPingSenderEvents
import com.gojek.mqtt.pingsender.KeepAlive
import com.gojek.mqtt.pingsender.KeepAliveCalculator
import com.gojek.mqtt.pingsender.NoOpPingSenderEvents
import com.gojek.mqtt.pingsender.keepAliveMillis
import org.eclipse.paho.client.mqttv3.ILogger
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.internal.ClientComms

internal class WorkManagerPingSenderAdaptive(
    private val pingWorkScheduler: PingWorkScheduler,
    private val pingSenderConfig: WorkManagerPingSenderConfig,
    private val clock: Clock = Clock()
) : AdaptiveMqttPingSender {
    private lateinit var comms: ClientComms
    private lateinit var logger: ILogger

    private lateinit var keepAliveCalculator: KeepAliveCalculator

    @VisibleForTesting
    internal lateinit var adaptiveKeepAlive: KeepAlive

    private var pingSenderEvents: IPingSenderEvents = NoOpPingSenderEvents()

    override fun setKeepAliveCalculator(keepAliveCalculator: KeepAliveCalculator) {
        this.keepAliveCalculator = keepAliveCalculator
    }

    override fun init(
        comms: ClientComms,
        logger: ILogger
    ) {
        pingSender = this
        this.comms = comms
        this.logger = logger
    }

    override fun start() {
        logger.d(TAG, "Starting work manager ping sender")
        schedule(comms.keepAlive)
    }

    override fun stop() {
        logger.d(TAG, "Stopping work manager ping sender")
        pingWorkScheduler.cancelWork()
    }

    override fun schedule(ignoredDelay: Long) {
        adaptiveKeepAlive = keepAliveCalculator.getUnderTrialKeepAlive()
        val delayInMilliseconds = adaptiveKeepAlive.keepAliveMillis()

        pingWorkScheduler.schedulePingWork(delayInMilliseconds, pingSenderConfig.timeoutSeconds)

        pingSenderEvents.mqttPingScheduled(delayInMilliseconds.fromMillisToSeconds(), delayInMilliseconds.fromMillisToSeconds())
    }

    override fun setPingEventHandler(pingSenderEvents: IPingSenderEvents) {
        this.pingSenderEvents = pingSenderEvents
    }

    fun sendPing(onComplete: (success: Boolean) -> Unit) {
        val serverUri = comms.client?.serverURI ?: ""
        val keepAliveMillis = adaptiveKeepAlive.keepAliveMillis()
        pingSenderEvents.mqttPingInitiated(
            serverUri,
            keepAliveMillis.fromMillisToSeconds()
        )
        val token: IMqttToken? = comms.sendPingRequest()
        if (token == null) {
            logger.d(TAG, "Mqtt Ping Token null")
            pingSenderEvents.pingMqttTokenNull(
                serverUri,
                adaptiveKeepAlive.keepAliveMillis().fromMillisToSeconds()
            )
            return
        }
        val sTime = clock.nanoTime()
        token.userContext = adaptiveKeepAlive
        token.actionCallback = object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken) {
                logger.d(TAG, "Mqtt Ping Sent successfully")
                val timeTaken = (clock.nanoTime() - sTime).fromNanosToMillis()
                pingSenderEvents.pingEventSuccess(
                    serverUri,
                    timeTaken,
                    keepAliveMillis.fromMillisToSeconds()
                )
                keepAliveCalculator.onKeepAliveSuccess(asyncActionToken.userContext as KeepAlive)
                schedule(0)
                onComplete(true)
            }

            override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                logger.d(TAG, "Mqtt Ping Sent failed")
                val timeTaken = (clock.nanoTime() - sTime).fromNanosToMillis()
                pingSenderEvents.pingEventFailure(
                    serverUri,
                    timeTaken,
                    exception,
                    keepAliveMillis.fromMillisToSeconds()
                )
                keepAliveCalculator.onKeepAliveFailure(asyncActionToken.userContext as KeepAlive)
                onComplete(false)
            }
        }
    }

    companion object {
        @Volatile
        var pingSender: WorkManagerPingSenderAdaptive? = null
    }
}

private const val TAG = "WorkManagerPingSender"
