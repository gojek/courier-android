package com.gojek.mqtt.client.internal

import com.gojek.mqtt.model.KeepAlive
import com.gojek.mqtt.model.MqttConnectOptions

internal interface KeepAliveProvider {
    fun getKeepAlive(connectOptions: MqttConnectOptions): KeepAlive
}

internal class NonAdaptiveKeepAliveProvider : KeepAliveProvider {
    override fun getKeepAlive(connectOptions: MqttConnectOptions): KeepAlive {
        return connectOptions.keepAlive
    }
}

internal class OptimalKeepAliveProvider(
    keepAliveSeconds: Int
) : KeepAliveProvider {
    private val keepAlive = KeepAlive(keepAliveSeconds, true)
    override fun getKeepAlive(connectOptions: MqttConnectOptions): KeepAlive {
        return keepAlive
    }
}

internal class NonOptimalKeepAliveProvider(
    keepAliveSeconds: Int
) : KeepAliveProvider {
    private val keepAlive = KeepAlive(keepAliveSeconds, false)
    override fun getKeepAlive(connectOptions: MqttConnectOptions): KeepAlive {
        return keepAlive
    }
}
