package com.gojek.keepalive.model

import com.gojek.mqtt.pingsender.KeepAlive
import com.google.gson.annotations.SerializedName

internal data class KeepAlivePersistenceModel(
    @SerializedName("lastSuccessfulKeepAlive")
    val lastSuccessfulKeepAlive: Int,
    @SerializedName("underTrialKeepAlive")
    val underTrialKeepAlive: Int,
    @SerializedName("keepAliveFailureCount")
    val keepAliveFailureCount: Int,
    @SerializedName("nwType")
    val networkType: Int,
    @SerializedName("nwName")
    val networkName: String,
    @SerializedName("lowerBound")
    val lowerBound: Int,
    @SerializedName("upperBound")
    val upperBound: Int,
    @SerializedName("step")
    val step: Int,
    @SerializedName("isOptimalKA")
    val isOptimalKeepAlive: Boolean,
    @SerializedName("probeCount")
    val probeCount: Int,
    @SerializedName("convergenceTime")
    val convergenceTime: Int
)

internal fun KeepAlivePersistenceModel.toKeepAlive(): KeepAlive {
    return KeepAlive(networkType, networkName, underTrialKeepAlive)
}