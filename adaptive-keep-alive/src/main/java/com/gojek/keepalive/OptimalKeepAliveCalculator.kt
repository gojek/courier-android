package com.gojek.keepalive

import androidx.annotation.VisibleForTesting
import com.gojek.keepalive.model.KeepAlivePersistenceModel
import com.gojek.keepalive.model.toKeepAlive
import com.gojek.keepalive.persistence.KeepAlivePersistence
import com.gojek.keepalive.utils.NetworkUtils
import com.gojek.mqtt.pingsender.KeepAlive
import com.gojek.mqtt.pingsender.KeepAliveCalculator
import com.google.gson.Gson
import java.util.*

internal class OptimalKeepAliveCalculator(
    private val networkUtils: NetworkUtils,
    private val lowerBound: Int,
    private val upperBound: Int,
    private val step: Int = 1,
    private val optimalKeepAliveResetLimit: Int,
    private val persistence: KeepAlivePersistence,
    private val optimalKeepAliveObserver: OptimalKeepAliveObserver,
    private val gson: Gson
) : KeepAliveCalculator {
    @VisibleForTesting
    internal var lastSuccessfulKA = lowerBound - step

    @VisibleForTesting
    internal var isOptimalKeepAlive = false
    @VisibleForTesting
    internal var optimalKAFailureCount = 0

    @VisibleForTesting
    internal var currentUpperBound = upperBound
    @VisibleForTesting
    internal var currentStep = step

    @VisibleForTesting
    internal var currentNetworkType: Int = -1
    @VisibleForTesting
    internal var currentNetworkName: String = ""

    @VisibleForTesting
    internal var currentKA = -1
    @VisibleForTesting
    internal var currentKAFailureCount = 0

    @VisibleForTesting
    internal var probeCount = 0
    @VisibleForTesting
    internal var convergenceTime = 0

    override fun init() {
        val networkType = networkUtils.getNetworkType().toInt()
        val networkName = networkUtils.getNetworkName()
        val nwName = networkName.toLowerCase(Locale.getDefault())
        if (currentNetworkName != nwName || currentNetworkType != networkType) {
            this.currentNetworkType = networkType
            this.currentNetworkName = nwName
            optimalKAFailureCount = 0
            if (persistence.has(getNetworkKey())) {
                val keepAliveString = persistence.get(getNetworkKey(), "")
                val keepAlive = gson.fromJson(keepAliveString, KeepAlivePersistenceModel::class.java)
                this.lastSuccessfulKA = keepAlive.lastSuccessfulKeepAlive
                this.isOptimalKeepAlive = keepAlive.isOptimalKeepAlive
                this.currentUpperBound = keepAlive.upperBound
                this.currentStep = keepAlive.step
                this.currentKA = keepAlive.underTrialKeepAlive
                this.currentKAFailureCount = keepAlive.keepAliveFailureCount
                this.probeCount = keepAlive.probeCount
                this.convergenceTime = keepAlive.convergenceTime
                if (keepAlive.upperBound != upperBound) {
                    this.currentUpperBound = upperBound
                    this.isOptimalKeepAlive = false
                    probeCount = 0
                    convergenceTime = 0
                    this.currentStep = step
                }
                if (keepAlive.lowerBound != lowerBound) {
                    this.isOptimalKeepAlive = false
                    probeCount = 0
                    convergenceTime = 0
                    this.currentStep = step
                }
            } else {
                lastSuccessfulKA = lowerBound - step
                isOptimalKeepAlive = false
                currentUpperBound = upperBound
                currentStep = step
                currentKA = -1
                currentKAFailureCount = 0
                probeCount = 0
                convergenceTime = 0
            }
        }
    }

    override fun getKeepAlive(): KeepAlive {
        if (isOptimalKeepAlive.not()) {
            calculateKeepAlive()
        }
        val keepAlivePersistenceModel = getKeepAlivePersistenceModel()
        persistence.put(getNetworkKey(), gson.toJson(keepAlivePersistenceModel))
        return keepAlivePersistenceModel.toKeepAlive()
    }

    @VisibleForTesting
    internal fun calculateKeepAlive() {
        if (lastSuccessfulKA == currentUpperBound) {
            isOptimalKeepAlive = true
            this.currentKAFailureCount = 0
            persistence.put(getNetworkKey(), gson.toJson(getKeepAlivePersistenceModel()))
            optimalKeepAliveObserver.onOptimalKeepAliveFound(
                lastSuccessfulKA,
                probeCount,
                convergenceTime
            )
        } else {
            val keepAlive = minOf(lastSuccessfulKA + currentStep, currentUpperBound)
            if (keepAlive == currentKA) {
                ++currentKAFailureCount
            } else {
                currentKA = keepAlive
                currentKAFailureCount = 0
            }
            if (currentKAFailureCount >= MAX_CURRENT_KEEPALIVE_FAILURE) {
                onKeepAliveFailure(getCurrentKeepAlive())
                calculateKeepAlive()
            }
            ++probeCount
            convergenceTime += currentKA
        }
    }

    override fun onKeepAliveSuccess(keepAlive: KeepAlive) {
        if (keepAlive.networkType == currentNetworkType
            && keepAlive.networkName == currentNetworkName
        ) {
            this.lastSuccessfulKA = keepAlive.underTrialKeepAlive
            this.currentKAFailureCount = 0
            persistence.put(getNetworkKey(), gson.toJson(getKeepAlivePersistenceModel()))
        }
    }

    override fun onKeepAliveFailure(keepAlive: KeepAlive) {
        if (keepAlive.networkType == currentNetworkType
            && keepAlive.networkName == currentNetworkName
        ) {
            if (isOptimalKeepAlive) {
                if (++optimalKAFailureCount >= optimalKeepAliveResetLimit) {
                    persistence.remove(getNetworkKey())
                }
            } else if (keepAlive.underTrialKeepAlive == lowerBound) {
                isOptimalKeepAlive = true
                lastSuccessfulKA = lowerBound
                this.currentKAFailureCount = 0
                persistence.put(getNetworkKey(), gson.toJson(getKeepAlivePersistenceModel()))
                optimalKeepAliveObserver.onOptimalKeepAliveNotFound(
                    keepAlive.underTrialKeepAlive,
                    probeCount,
                    convergenceTime
                )
            } else {
                currentUpperBound = keepAlive.underTrialKeepAlive - 1
                if (currentStep > 1) {
                    currentStep /= 2
                }
            }
        }
    }

    override fun getOptimalKeepAlive(): Int {
        if (isOptimalKeepAlive.not()) {
            return 0
        }
        return lastSuccessfulKA
    }

    override fun onOptimalKeepAliveFailure() {
        if (isOptimalKeepAlive) {
            if (++optimalKAFailureCount >= optimalKeepAliveResetLimit) {
                persistence.remove(getNetworkKey())
            }
        }
    }

    @VisibleForTesting
    internal fun getNetworkKey(): String {
        return "$currentNetworkType:$currentNetworkName"
    }

    @VisibleForTesting
    internal fun getKeepAlivePersistenceModel(): KeepAlivePersistenceModel {
        return KeepAlivePersistenceModel(
            lastSuccessfulKeepAlive = lastSuccessfulKA,
            networkType = currentNetworkType,
            networkName = currentNetworkName,
            lowerBound = lowerBound,
            upperBound = upperBound,
            isOptimalKeepAlive = isOptimalKeepAlive,
            step = currentStep,
            underTrialKeepAlive = currentKA,
            keepAliveFailureCount = currentKAFailureCount,
            probeCount = probeCount,
            convergenceTime = convergenceTime
        )
    }

    @VisibleForTesting
    internal fun getCurrentKeepAlive(): KeepAlive {
        return KeepAlive(
            networkType = currentNetworkType,
            networkName = currentNetworkName,
            underTrialKeepAlive = currentKA
        )
    }

    companion object {
        const val MAX_CURRENT_KEEPALIVE_FAILURE = 3
    }
}