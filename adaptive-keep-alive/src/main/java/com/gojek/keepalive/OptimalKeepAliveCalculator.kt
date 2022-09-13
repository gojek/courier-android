package com.gojek.keepalive

import androidx.annotation.VisibleForTesting
import com.gojek.keepalive.utils.NetworkUtils
import com.gojek.mqtt.pingsender.KeepAlive
import com.gojek.mqtt.pingsender.KeepAliveCalculator
import com.gojek.networktracker.NetworkStateListener
import com.gojek.networktracker.NetworkStateTracker
import com.gojek.networktracker.model.NetworkState

internal class OptimalKeepAliveCalculator(
    networkTracker: NetworkStateTracker,
    private val networkUtils: NetworkUtils,
    private val stateHandler: AdaptiveKeepAliveStateHandler,
    private val optimalKeepAliveObserver: OptimalKeepAliveObserver
) : KeepAliveCalculator {

    @VisibleForTesting
    internal val networkStateListener: NetworkStateListener =
        object : NetworkStateListener {
            override fun onStateChanged(activeNetworkState: NetworkState) {
                synchronized(this) {
                    val networkType = networkUtils.getNetworkType(activeNetworkState.netInfo)
                    val networkName = networkUtils.getNetworkName(activeNetworkState.netInfo)
                    onNetworkStateChanged(networkType, networkName)
                }
            }
        }

    init {
        networkTracker.addListener(networkStateListener)
    }

    @Synchronized
    private fun onNetworkStateChanged(networkType: Int, networkName: String) {
        stateHandler.onNetworkChanged(networkType, networkName)
    }

    @Synchronized
    override fun getUnderTrialKeepAlive(): KeepAlive {
        return if (stateHandler.isOptimalKeepAliveFound()) {
            stateHandler.getOptimalKeepAlive()
        } else {
            calculateKeepAlive()
        }
    }

    private fun calculateKeepAlive(): KeepAlive {
        stateHandler.calculateNextKeepAlive()
        return if (stateHandler.isCurrentKeepAliveFailureLimitExceeded()) {
            handleKeepAliveFailure(stateHandler.getCurrentKeepAlive())
            getUnderTrialKeepAlive()
        } else {
            stateHandler.updateProbeCountAndConvergenceTime()
            stateHandler.updatePersistenceWithLatestState()
            stateHandler.getCurrentKeepAlive()
        }
    }

    @Synchronized
    override fun onKeepAliveSuccess(keepAlive: KeepAlive) {
        if (stateHandler.isValidKeepAlive(keepAlive)) {
            stateHandler.updateKeepAliveSuccessState(keepAlive)
            if (stateHandler.isOptimalKeepAliveFound()) {
                optimalKeepAliveObserver.onOptimalKeepAliveFound(
                    timeMinutes = stateHandler.getOptimalKeepAlive().keepAliveMinutes,
                    probeCount = stateHandler.getProbeCount(),
                    convergenceTime = stateHandler.getConvergenceTime()
                )
            }
            stateHandler.updatePersistenceWithLatestState()
        }
    }

    @Synchronized
    override fun onKeepAliveFailure(keepAlive: KeepAlive) {
        if (stateHandler.isValidKeepAlive(keepAlive)) {
            handleKeepAliveFailure(keepAlive)
        }
    }

    @VisibleForTesting
    internal fun handleKeepAliveFailure(keepAlive: KeepAlive) {
        stateHandler.updateKeepAliveFailureState(keepAlive)
        if (stateHandler.isOptimalKeepAliveFound()) {
            optimalKeepAliveObserver.onOptimalKeepAliveFound(
                timeMinutes = stateHandler.getOptimalKeepAlive().keepAliveMinutes,
                probeCount = stateHandler.getProbeCount(),
                convergenceTime = stateHandler.getConvergenceTime()
            )
        }
        stateHandler.updatePersistenceWithLatestState()
    }

    @Synchronized
    override fun getOptimalKeepAlive(): Int {
        if (stateHandler.isOptimalKeepAliveFound().not()) {
            return 0
        }
        return stateHandler.getOptimalKeepAlive().keepAliveMinutes
    }

    @Synchronized
    override fun onOptimalKeepAliveFailure() {
        if (stateHandler.isOptimalKeepAliveFound()) {
            stateHandler.updateOptimalKeepAliveFailureState()
            if (stateHandler.isOptimalKeepAliveFailureLimitExceeded()) {
                stateHandler.removeStateFromPersistence()
                stateHandler.resetState()
            }
        }
    }
}
