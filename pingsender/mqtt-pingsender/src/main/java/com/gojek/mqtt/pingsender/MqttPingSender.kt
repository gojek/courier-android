package com.gojek.mqtt.pingsender

import androidx.annotation.RestrictTo
import org.eclipse.paho.client.mqtt.ILogger
import org.eclipse.paho.client.mqtt.internal.IClientComms
import org.eclipse.paho.client.mqtt.MqttPingSender as PahoPingSender

interface MqttPingSender {
    fun init(comms: IClientComms, logger: ILogger)
    fun start()
    fun stop()
    fun schedule(delayInMilliseconds: Long)

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun setPingEventHandler(pingSenderEvents: IPingSenderEvents)
}

@RestrictTo(RestrictTo.Scope.LIBRARY)
fun MqttPingSender.toPahoPingSender(): PahoPingSender {
    val mqttPingSender = this
    return object : PahoPingSender {
        override fun init(comms: IClientComms, logger: ILogger) {
            mqttPingSender.init(comms, logger)
        }

        override fun start() {
            mqttPingSender.start()
        }

        override fun stop() {
            mqttPingSender.stop()
        }

        override fun schedule(delayInMilliseconds: Long) {
            mqttPingSender.schedule(delayInMilliseconds)
        }
    }
}
