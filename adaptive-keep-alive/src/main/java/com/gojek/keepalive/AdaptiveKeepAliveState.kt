package com.gojek.keepalive

internal data class AdaptiveKeepAliveState(
    val lastSuccessfulKA: Int,
    val isOptimalKeepAlive: Boolean,
    val optimalKAFailureCount: Int,
    val currentUpperBound: Int,
    val currentStep: Int,
    val currentNetworkType: Int,
    val currentNetworkName: String,
    val currentKA: Int,
    val currentKAFailureCount: Int,
    val probeCount: Int,
    val convergenceTime: Int,
    val lowerBound: Int,
    val upperBound: Int,
    val step: Int,
    val optimalKeepAliveResetLimit: Int,
)