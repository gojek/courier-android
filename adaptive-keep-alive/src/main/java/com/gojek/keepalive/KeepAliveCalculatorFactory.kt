package com.gojek.keepalive

import android.content.Context
import androidx.annotation.RestrictTo
import com.gojek.keepalive.config.AdaptiveKeepAliveConfig
import com.gojek.keepalive.constants.KEEP_ALIVE_PERSISTENCE
import com.gojek.keepalive.persistence.KeepAlivePersistenceImpl
import com.gojek.keepalive.sharedpref.CourierSharedPreferencesFactory
import com.gojek.keepalive.utils.NetworkUtils
import com.gojek.mqtt.pingsender.KeepAliveCalculator
import com.google.gson.Gson

@RestrictTo(RestrictTo.Scope.LIBRARY)
class KeepAliveCalculatorFactory {
    fun create(
        context: Context,
        adaptiveKeepAliveConfig: AdaptiveKeepAliveConfig,
        optimalKeepAliveObserver: OptimalKeepAliveObserver,
    ): KeepAliveCalculator {
        val sharedPreferences = CourierSharedPreferencesFactory.create(context, KEEP_ALIVE_PERSISTENCE)
        return OptimalKeepAliveCalculator(
            networkUtils = NetworkUtils(context),
            lowerBound = adaptiveKeepAliveConfig.lowerBoundMinutes,
            upperBound = adaptiveKeepAliveConfig.upperBoundMinutes,
            step = adaptiveKeepAliveConfig.stepMinutes,
            optimalKeepAliveResetLimit = adaptiveKeepAliveConfig.optimalKeepAliveResetLimit,
            persistence = KeepAlivePersistenceImpl(sharedPreferences),
            optimalKeepAliveObserver = optimalKeepAliveObserver,
            gson = Gson()
        )
    }
}