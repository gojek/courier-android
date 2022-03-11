package com.gojek.mqtt.network

import android.content.Context
import android.os.Build
import com.gojek.courier.logging.ILogger
import com.gojek.mqtt.client.v3.IAndroidMqttClient
import com.gojek.mqtt.utils.NetworkUtils
import com.gojek.networktracker.NetworkStateListener
import com.gojek.networktracker.NetworkStateTracker
import com.gojek.networktracker.model.NetworkState

internal class NetworkHandler(
    private val context: Context,
    private val logger: ILogger,
    private val androidMqttClient: IAndroidMqttClient,
    private val networkUtils: NetworkUtils,
    private val isNetworkValidatedCheckEnabled: Boolean,
    private val networkStateTracker: NetworkStateTracker
) {

    private val networkStateListener: NetworkStateListener =
        object : NetworkStateListener {
            override fun onStateChanged(activeNetworkState: NetworkState) {
                val previousNetworkState = networkState
                logger.d("NetworkHandler", "Network state changed: $activeNetworkState")
                logger.d("NetworkHandler", "Previous network state: $previousNetworkState")
                if (activeNetworkState.isConnected(isNetworkValidatedCheckEnabled)) {
                    if(androidMqttClient.isConnected().not()) {
                        logger.d("NetworkHandler", "connecting mqtt on network connect")
                        androidMqttClient.connect()
                    } else if (previousNetworkState.isConnected(isNetworkValidatedCheckEnabled).not()) {
                        logger.d("NetworkHandler", "reconnecting mqtt on network connect")
                        androidMqttClient.reconnect()
                    }
                }
                networkState = activeNetworkState
            }
        }

    private var networkState = networkStateTracker.getActiveNetworkState()

    fun init() {
        networkStateTracker.addListener(networkStateListener)
    }

    fun destroy() {
        networkStateTracker.removeListener(networkStateListener)
    }

    fun isConnected(): Boolean {
        return networkState.isConnected(isNetworkValidatedCheckEnabled)
    }

    fun getActiveNetworkInfo(): ActiveNetInfo {
        return ActiveNetInfo(
            connected = networkState.isConnected,
            validated = networkState.isValidated,
            networkType = networkUtils.getNetworkType(networkState.netInfo)
        )
    }
}

private fun NetworkState.isConnected(isNetworkValidatedCheckEnabled: Boolean): Boolean {
    return if (isNetworkValidatedCheckEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        this.isConnected && this.isValidated
    } else {
        this.isConnected
    }
}