package com.gojek.mqtt.pingsender

import androidx.annotation.RestrictTo

interface AdaptiveMqttPingSender : MqttPingSender {
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun setKeepAliveCalculator(keepAliveCalculator: KeepAliveCalculator)
}
