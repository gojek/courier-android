package com.gojek.mqtt.model

data class KeepAlive(
    val timeSeconds: Int,
    internal val isOptimal: Boolean = false
) {
    companion object {
        val NO_KEEP_ALIVE = KeepAlive(timeSeconds = 60, isOptimal = false)
    }
}
