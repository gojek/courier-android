package com.gojek.workmanager.pingsender

import com.gojek.courier.extensions.fromMillisToSeconds
import com.gojek.courier.extensions.fromNanosToMillis
import com.gojek.courier.utils.Clock
import com.gojek.courier.logging.ILogger
import com.gojek.mqtt.pingsender.IPingSenderEvents
import com.gojek.mqtt.pingsender.MqttPingSender
import com.gojek.mqtt.pingsender.NoOpPingSenderEvents
import com.gojek.mqtt.pingsender.PingActionCallback
import com.gojek.mqtt.pingsender.PingSenderComms

internal class WorkManagerPingSender(
    private val pingWorkScheduler: PingWorkScheduler,
    private val pingSenderConfig: WorkManagerPingSenderConfig,
    private val clock: Clock = Clock()
) : MqttPingSender {
    private lateinit var comms: PingSenderComms
    private lateinit var logger: ILogger

    private var pingSenderEvents: IPingSenderEvents = NoOpPingSenderEvents()

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

    override fun schedule(delayInMilliseconds: Long) {
        pingWorkScheduler.schedulePingWork(delayInMilliseconds, pingSenderConfig.timeoutSeconds)
        pingSenderEvents.mqttPingScheduled(delayInMilliseconds.fromMillisToSeconds(), comms.keepAliveMillis.fromMillisToSeconds())
    }

    override fun setPingEventHandler(pingSenderEvents: IPingSenderEvents) {
        this.pingSenderEvents = pingSenderEvents
    }

    fun sendPing(onComplete: (success: Boolean) -> Unit) {
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
                    onComplete(false)
                }
            }
        )
        if (!initiated) {
            logger.d(TAG, "Mqtt Ping Token null")
            pingSenderEvents.pingMqttTokenNull(serverUri, keepAliveMillis.fromMillisToSeconds())
            return
        }
    }

    companion object {
        @Volatile
        var pingSender: WorkManagerPingSender? = null
    }
}

private const val TAG = "WorkManagerPingSender"
