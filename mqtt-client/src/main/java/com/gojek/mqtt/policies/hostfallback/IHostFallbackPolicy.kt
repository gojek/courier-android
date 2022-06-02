package com.gojek.mqtt.policies.hostfallback

import com.gojek.mqtt.model.ServerUri
import com.gojek.mqtt.policies.IFallbackPolicy

interface IHostFallbackPolicy : IFallbackPolicy {
    fun getServerUri(): ServerUri
    fun onConnectFailure(exception: Throwable)
}
