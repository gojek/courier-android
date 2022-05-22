package com.gojek.workmanager.pingsender

import com.gojek.courier.extensions.fromMillisToSeconds
import com.gojek.courier.extensions.fromNanosToMillis
import com.gojek.courier.utils.Clock
import com.gojek.mqtt.pingsender.IPingSenderEvents
import com.gojek.mqtt.pingsender.MqttPingSender
import com.gojek.mqtt.pingsender.NoOpPingSenderEvents
import org.eclipse.paho.client.mqttv3.ILogger
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.internal.ClientComms

internal class WorkManagerPingSender(
    private val pingWorkScheduler: PingWorkScheduler,
    private val pingSenderConfig: WorkManagerPingSenderConfig,
    private val clock: Clock = Clock()
) : MqttPingSender {
    private lateinit var comms: ClientComms
    private lateinit var logger: ILogger

    private var pingSenderEvents: IPingSenderEvents = NoOpPingSenderEvents()

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

    override fun schedule(delayInMilliseconds: Long) {
        pingWorkScheduler.schedulePingWork(delayInMilliseconds, pingSenderConfig.timeoutSeconds)
        pingSenderEvents.mqttPingScheduled(delayInMilliseconds.fromMillisToSeconds(), comms.keepAlive.fromMillisToSeconds())
    }

    override fun setPingEventHandler(pingSenderEvents: IPingSenderEvents) {
        this.pingSenderEvents = pingSenderEvents
    }

    fun sendPing(onComplete: (success: Boolean) -> Unit) {
        val serverUri = comms.client?.serverURI ?: ""
        val keepAliveMillis = comms.keepAlive
        pingSenderEvents.mqttPingInitiated(serverUri, keepAliveMillis.fromMillisToSeconds())

        val token = comms.checkForActivity()
        if (token == null) {
            logger.d(TAG, "Mqtt Ping Token null")
            pingSenderEvents.pingMqttTokenNull(serverUri, keepAliveMillis.fromMillisToSeconds())
            return
        }
        val sTime = clock.nanoTime()
        token.actionCallback = object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken) {
                logger.d(TAG, "Mqtt Ping Sent successfully")
                val timeTaken = (clock.nanoTime() - sTime).fromNanosToMillis()
                pingSenderEvents.pingEventSuccess(serverUri, timeTaken, keepAliveMillis.fromMillisToSeconds())
                onComplete(true)
            }

            override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                logger.d(TAG, "Mqtt Ping Sent failed")
                val timeTaken = (clock.nanoTime() - sTime).fromNanosToMillis()
                pingSenderEvents.pingEventFailure(
                    serverUri,
                    timeTaken,
                    exception,
                    comms.keepAlive.fromMillisToSeconds()
                )
                onComplete(false)
            }
        }
    }

    companion object {
        @Volatile
        var pingSender: WorkManagerPingSender? = null
    }
}

private const val TAG = "WorkManagerPingSender"
