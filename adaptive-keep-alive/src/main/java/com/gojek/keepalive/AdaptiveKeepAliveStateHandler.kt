package com.gojek.keepalive

import androidx.annotation.VisibleForTesting
import com.gojek.keepalive.model.KeepAlivePersistenceModel
import com.gojek.keepalive.persistence.KeepAlivePersistence
import com.gojek.mqtt.pingsender.KeepAlive
import java.util.Locale

internal class AdaptiveKeepAliveStateHandler(
    lowerBound: Int,
    upperBound: Int,
    step: Int = 1,
    optimalKeepAliveResetLimit: Int,
    private val persistence: KeepAlivePersistence
) {
    @VisibleForTesting
    @Volatile
    internal var state = AdaptiveKeepAliveState(
        lastSuccessfulKA = lowerBound - step,
        isOptimalKeepAlive = false,
        currentUpperBound = upperBound,
        currentStep = step,
        currentKA = -1,
        currentKAFailureCount = 0,
        probeCount = 0,
        convergenceTime = 0,
        optimalKAFailureCount = 0,
        currentNetworkName = "",
        currentNetworkType = -1,
        lowerBound = lowerBound,
        upperBound = upperBound,
        step = step,
        optimalKeepAliveResetLimit = optimalKeepAliveResetLimit
    )

    fun onNetworkChanged(networkType: Int, networkName: String) {
        val nwName = networkName.toLowerCase(Locale.getDefault())
        if (state.currentNetworkName != nwName || state.currentNetworkType != networkType) {
            resetState()
            state = state.copy(
                currentNetworkType = networkType,
                currentNetworkName = nwName
            )
            if (persistence.has(getNetworkKey())) {
                val keepAlive = persistence.get(getNetworkKey())
                val currentUpperBound = if (keepAlive.currentUpperBound > 0) keepAlive.currentUpperBound else keepAlive.upperBound
                state = state.copy(
                    lastSuccessfulKA = keepAlive.lastSuccessfulKeepAlive,
                    isOptimalKeepAlive = keepAlive.isOptimalKeepAlive,
                    currentStep = keepAlive.step,
                    currentKA = keepAlive.underTrialKeepAlive,
                    currentKAFailureCount = keepAlive.keepAliveFailureCount,
                    probeCount = keepAlive.probeCount,
                    convergenceTime = keepAlive.convergenceTime,
                    currentUpperBound = currentUpperBound
                )
                if (keepAlive.upperBound != state.upperBound) {
                    state = state.copy(
                        currentUpperBound = state.upperBound,
                        isOptimalKeepAlive = false,
                        probeCount = 0,
                        convergenceTime = 0,
                        currentStep = state.step
                    )
                }
                if (keepAlive.lowerBound != state.lowerBound) {
                    state = state.copy(
                        isOptimalKeepAlive = false,
                        probeCount = 0,
                        convergenceTime = 0,
                        currentStep = state.step
                    )
                }
            } else {
                resetState()
            }
        }
    }

    fun updateKeepAliveSuccessState(keepAlive: KeepAlive) {
        state = state.copy(
            lastSuccessfulKA = keepAlive.keepAliveMinutes,
            currentKAFailureCount = 0
        )
        if (state.lastSuccessfulKA == state.currentUpperBound) {
            state = state.copy(
                isOptimalKeepAlive = true
            )
        }
    }

    fun updateKeepAliveFailureState(keepAlive: KeepAlive) {
        if (keepAlive.keepAliveMinutes == state.lowerBound) {
            state = state.copy(
                isOptimalKeepAlive = true,
                lastSuccessfulKA = state.lowerBound,
                currentKAFailureCount = 0
            )
        } else {
            val currentUpperBound = keepAlive.keepAliveMinutes - 1
            if (state.lastSuccessfulKA >= currentUpperBound) {
                state = state.copy(
                    currentUpperBound = currentUpperBound,
                    isOptimalKeepAlive = true,
                    currentKAFailureCount = 0
                )
            } else if (state.currentStep > 1) {
                state = state.copy(
                    currentUpperBound = currentUpperBound,
                    currentStep = state.currentStep / 2,
                    currentKAFailureCount = 0
                )
            }
        }
    }

    fun updateOptimalKeepAliveFailureState() {
        state = state.copy(
            optimalKAFailureCount = state.optimalKAFailureCount + 1
        )
    }

    fun updatePersistenceWithLatestState() {
        persistence.put(getNetworkKey(), getKeepAlivePersistenceModel())
    }

    fun removeStateFromPersistence() {
        persistence.remove(getNetworkKey())
    }

    fun getCurrentKeepAlive(): KeepAlive {
        return KeepAlive(
            networkType = state.currentNetworkType,
            networkName = state.currentNetworkName,
            keepAliveMinutes = state.currentKA
        )
    }

    fun calculateNextKeepAlive() {
        val keepAlive = minOf(state.lastSuccessfulKA + state.currentStep, state.currentUpperBound)
        state = if (keepAlive == state.currentKA) {
            state.copy(
                currentKAFailureCount = state.currentKAFailureCount + 1
            )
        } else {
            state.copy(
                currentKA = keepAlive,
                currentKAFailureCount = 0
            )
        }
    }

    fun isCurrentKeepAliveFailureLimitExceeded(): Boolean {
        return state.currentKAFailureCount >= MAX_CURRENT_KEEPALIVE_FAILURE
    }

    fun isOptimalKeepAliveFailureLimitExceeded(): Boolean {
        return state.optimalKAFailureCount >= state.optimalKeepAliveResetLimit
    }

    fun isOptimalKeepAliveFound(): Boolean {
        return state.isOptimalKeepAlive
    }

    fun getOptimalKeepAlive(): KeepAlive {
        return KeepAlive(
            networkType = state.currentNetworkType,
            networkName = state.currentNetworkName,
            keepAliveMinutes = state.lastSuccessfulKA
        )
    }

    fun getProbeCount(): Int {
        return state.probeCount
    }

    fun getConvergenceTime(): Int {
        return state.convergenceTime
    }

    fun updateProbeCountAndConvergenceTime() {
        state = state.copy(
            probeCount = state.probeCount + 1,
            convergenceTime = state.convergenceTime + state.currentKA
        )
    }

    fun isValidKeepAlive(keepAlive: KeepAlive): Boolean {
        return keepAlive.networkType == state.currentNetworkType &&
            keepAlive.networkName == state.currentNetworkName &&
            keepAlive.keepAliveMinutes == state.currentKA
    }

    fun resetState() {
        state = state.copy(
            lastSuccessfulKA = state.lowerBound - state.step,
            isOptimalKeepAlive = false,
            currentUpperBound = state.upperBound,
            currentStep = state.step,
            currentKA = -1,
            currentKAFailureCount = 0,
            probeCount = 0,
            convergenceTime = 0,
            optimalKAFailureCount = 0
        )
    }

    @VisibleForTesting
    internal fun getNetworkKey(): String {
        return "${state.currentNetworkType}:${state.currentNetworkName}"
    }

    @VisibleForTesting
    internal fun getKeepAlivePersistenceModel(): KeepAlivePersistenceModel {
        return with(state) {
            KeepAlivePersistenceModel(
                lastSuccessfulKeepAlive = lastSuccessfulKA,
                networkType = currentNetworkType,
                networkName = currentNetworkName,
                lowerBound = lowerBound,
                upperBound = upperBound,
                currentUpperBound = currentUpperBound,
                isOptimalKeepAlive = isOptimalKeepAlive,
                step = currentStep,
                underTrialKeepAlive = currentKA,
                keepAliveFailureCount = currentKAFailureCount,
                probeCount = probeCount,
                convergenceTime = convergenceTime
            )
        }
    }

    companion object {
        @VisibleForTesting
        internal const val MAX_CURRENT_KEEPALIVE_FAILURE = 3
    }
}
