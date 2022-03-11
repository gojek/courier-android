package com.gojek.mqtt.client.username

import com.gojek.mqtt.utils.DISCONNECTED
import com.gojek.mqtt.utils.NetworkUtils
import com.gojek.networktracker.NetworkStateTracker

internal class NetworkSectionProvider(private val networkStateTracker: NetworkStateTracker) :
    SectionProvider {
    override fun provideSection(): String {
        return try {
            NetworkUtils().getNetworkType(networkStateTracker.getActiveNetworkState().netInfo)
                .toString()
        } catch(th: Throwable) {
            DISCONNECTED.toString()
        }
    }
}
