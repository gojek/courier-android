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
import com.gojek.mqtt.pingsender.PingActionCallback
import com.gojek.mqtt.pingsender.PingSenderComms
import com.gojek.mqtt.pingsender.keepAliveMillis
import com.gojek.courier.logging.ILogger

internal class WorkManagerPingSenderAdaptive(
    private val pingWorkScheduler: PingWorkScheduler,
    private val pingSenderConfig: WorkManagerPingSenderConfig,
    private val clock: Clock = Clock()
) : AdaptiveMqttPingSender {
    private lateinit var comms: PingSenderComms
    private lateinit var logger: ILogger

    private lateinit var keepAliveCalculator: KeepAliveCalculator

    @VisibleForTesting
    internal lateinit var adaptiveKeepAlive: KeepAlive

    private var pingSenderEvents: IPingSenderEvents = NoOpPingSenderEvents()

    override fun setKeepAliveCalculator(keepAliveCalculator: KeepAliveCalculator) {
        this.keepAliveCalculator = keepAliveCalculator
    }

    override fun init(
        comms: PingSenderComms,
        logger: ILogger
    ) {
        pingSender = this
        this.comms = comms
        this.logger = logger
    }

    override fun start() {
        logger.d(TAG, "Starting work manager ping sender")
        schedule(comms.keepAliveMillis)
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
        val serverUri = comms.serverURI ?: ""
        val keepAlive = adaptiveKeepAlive
        val keepAliveMillis = adaptiveKeepAlive.keepAliveMillis()
        pingSenderEvents.mqttPingInitiated(
            serverUri,
            keepAliveMillis.fromMillisToSeconds()
        )
        val sTime = clock.nanoTime()
        val initiated = comms.sendPingRequest(
            object : PingActionCallback {
                override fun onSuccess() {
                    logger.d(TAG, "Mqtt Ping Sent successfully")
                    val timeTaken = (clock.nanoTime() - sTime).fromNanosToMillis()
                    pingSenderEvents.pingEventSuccess(
                        serverUri,
                        timeTaken,
                        keepAliveMillis.fromMillisToSeconds()
                    )
                    keepAliveCalculator.onKeepAliveSuccess(keepAlive)
                    schedule(0)
                    onComplete(true)
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
                    keepAliveCalculator.onKeepAliveFailure(keepAlive)
                    onComplete(false)
                }
            }
        )
        if (!initiated) {
            logger.d(TAG, "Mqtt Ping Token null")
            pingSenderEvents.pingMqttTokenNull(
                serverUri,
                adaptiveKeepAlive.keepAliveMillis().fromMillisToSeconds()
            )
            return
        }
    }

    companion object {
        @Volatile
        var pingSender: WorkManagerPingSenderAdaptive? = null
    }
}

private const val TAG = "WorkManagerPingSender"
