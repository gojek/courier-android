package com.gojek.keepalive

import android.content.Context
import androidx.annotation.RestrictTo
import com.gojek.courier.extensions.fromMinutesToSeconds
import com.gojek.keepalive.config.AdaptiveKeepAliveConfig

@RestrictTo(RestrictTo.Scope.LIBRARY)
class OptimalKeepAliveProvider(
    context: Context,
    adaptiveKeepAliveConfig: AdaptiveKeepAliveConfig,
    optimalKeepAliveObserver: OptimalKeepAliveObserver,
    keepAliveCalculatorFactory: KeepAliveCalculatorFactory = KeepAliveCalculatorFactory(),
) {

    val keepAliveCalculator = keepAliveCalculatorFactory.create(
        context = context,
        adaptiveKeepAliveConfig = adaptiveKeepAliveConfig,
        optimalKeepAliveObserver = optimalKeepAliveObserver
    )

    fun getOptimalKASecondsForCurrentNetwork(): Int {
        keepAliveCalculator.init()
        return keepAliveCalculator.getOptimalKeepAlive().fromMinutesToSeconds().toInt()
    }

    internal fun onOptimalKeepAliveFailure() {
        keepAliveCalculator.init()
        keepAliveCalculator.onOptimalKeepAliveFailure()
    }
}