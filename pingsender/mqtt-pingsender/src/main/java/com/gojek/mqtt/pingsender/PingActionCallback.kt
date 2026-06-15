package com.gojek.mqtt.pingsender

import androidx.annotation.RestrictTo

/**
 * Paho-agnostic callback invoked when a ping request initiated via [PingSenderComms]
 * completes. It abstracts away the underlying Paho action listener/token mechanism.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
interface PingActionCallback {
    fun onSuccess()
    fun onFailure(exception: Throwable)
}
