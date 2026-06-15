package com.gojek.mqtt.pingsender

import androidx.annotation.RestrictTo
import com.gojek.courier.logging.ILogger

interface MqttPingSender {
    fun init(comms: PingSenderComms, logger: ILogger)
    fun start()
    fun stop()
    fun schedule(delayInMilliseconds: Long)

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun setPingEventHandler(pingSenderEvents: IPingSenderEvents)
}
