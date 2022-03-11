package com.gojek.networktracker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.CONNECTIVITY_SERVICE
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.ConnectivityManager.CONNECTIVITY_ACTION
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.NonNull
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.core.net.ConnectivityManagerCompat
import com.gojek.courier.logging.ILogger
import com.gojek.networktracker.model.NetworkState
import com.gojek.networktracker.util.BuildInfoProvider

interface NetworkStateTracker {
    fun addListener(listener: NetworkStateListener)
    fun removeListener(listener: NetworkStateListener)
    fun getActiveNetworkState(): NetworkState
}

interface NetworkStateListener {
    fun onStateChanged(activeNetworkState: NetworkState)
}

internal class NetworkStateTrackerImpl(
    private val mAppContext: Context,
    private val logger: ILogger,
    private val buildInfoProvider: BuildInfoProvider = BuildInfoProvider()
): NetworkStateTracker {
    private val mConnectivityManager =
        mAppContext.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager

    @RequiresApi(Build.VERSION_CODES.N)
    @VisibleForTesting
    internal lateinit var mNetworkCallback: NetworkStateCallback
    @VisibleForTesting
    internal lateinit var mBroadcastReceiver: NetworkStateBroadcastReceiver

    private val mLock = Any()
    @VisibleForTesting
    internal val mListeners = linkedSetOf<NetworkStateListener>()

    @VisibleForTesting
    internal lateinit var mCurrentState: NetworkState

    init {
        if (isNetworkCallbackSupported()) {
            mNetworkCallback =
                NetworkStateCallback()
        } else {
            mBroadcastReceiver =
                NetworkStateBroadcastReceiver()
        }
    }

    /**
     * Add the given listener for tracking.
     * This may cause [.getInitialState] and [.startTracking] to be invoked.
     * If a state is set, this will immediately notify the given listener.
     *
     * @param listener The target listener to start notifying
     */
    override fun addListener(listener: NetworkStateListener) {
        synchronized(mLock) {
            if (mListeners.add(listener) && mListeners.size == 1) {
                mCurrentState = getInitialState()
                logger.d(
                    "NetworkStateTracker", String.format(
                        "%s: initial state = %s",
                        javaClass.simpleName,
                        mCurrentState
                    )
                )
                startTracking()
            }
            listener.onStateChanged(mCurrentState)
        }
    }

    /**
     * Remove the given listener from tracking.
     *
     * @param listener The listener to stop notifying.
     */
    override fun removeListener(listener: NetworkStateListener) {
        synchronized(mLock) {
            if (mListeners.remove(listener) && mListeners.isEmpty()) {
                stopTracking()
            }
        }
    }

    @VisibleForTesting
    internal fun getInitialState(): NetworkState {
        return getActiveNetworkState()
    }

    @VisibleForTesting
    internal fun startTracking() {
        if (isNetworkCallbackSupported()) {
            try {
                logger.d("NetworkStateTracker", "Registering network callback")
                mConnectivityManager.registerDefaultNetworkCallback(mNetworkCallback)
            } catch (e: IllegalArgumentException) {
                // Catching the exceptions since and moving on - this tracker is only used for
                // GreedyScheduler and there is nothing to be done about device-specific bugs.
                // IllegalStateException: Happening on NVIDIA Shield K1 Tablets.  See b/136569342.
                // SecurityException: Happening on Solone W1450.  See b/153246136.
                logger.e(
                    "NetworkStateTracker",
                    "Received exception while registering network callback",
                    e
                )
            } catch (e: SecurityException) {
                logger.e(
                    "NetworkStateTracker",
                    "Received exception while registering network callback",
                    e
                )
            }
        } else {
            logger.d("NetworkStateTracker", "Registering broadcast receiver")
            mAppContext.registerReceiver(
                mBroadcastReceiver,
                IntentFilter(CONNECTIVITY_ACTION)
            )
        }
    }

    @VisibleForTesting
    internal fun stopTracking() {
        if (isNetworkCallbackSupported()) {
            try {
                logger.d("NetworkStateTracker", "Unregistering network callback")
                mConnectivityManager.unregisterNetworkCallback(mNetworkCallback)
            } catch (e: IllegalArgumentException) {
                // Catching the exceptions since and moving on - this tracker is only used for
                // GreedyScheduler and there is nothing to be done about device-specific bugs.
                // IllegalStateException: Happening on NVIDIA Shield K1 Tablets.  See b/136569342.
                // SecurityException: Happening on Solone W1450.  See b/153246136.
                logger.e(
                    "NetworkStateTracker",
                    "Received exception while unregistering network callback",
                    e
                )
            } catch (e: SecurityException) {
                logger.e(
                    "NetworkStateTracker",
                    "Received exception while unregistering network callback",
                    e
                )
            }
        } else {
            logger.d("NetworkStateTracker", "Unregistering broadcast receiver")
            mAppContext.unregisterReceiver(mBroadcastReceiver)
        }
    }

    /**
     * Sets the state of the constraint.
     * If state is has not changed, nothing happens.
     *
     * @param newState new state of constraint
     */
    @VisibleForTesting
    internal fun setState(newState: NetworkState) {
        synchronized(mLock) {
            if (mCurrentState == newState) {
                return
            }
            mCurrentState = newState
            // onConstraintChanged may lead to calls to addListener or removeListener.
            // This can potentially result in a modification to the set while it is being
            // iterated over, so we handle this by creating a copy and using that for
            // iteration.
            val listenersList: List<NetworkStateListener> =
                ArrayList(mListeners)
            Handler(Looper.getMainLooper()).post {
                for (listener in listenersList) {
                    listener.onStateChanged(mCurrentState)
                }
            }
        }
    }


    @VisibleForTesting
    internal fun isNetworkCallbackSupported(): Boolean {
        return buildInfoProvider.isAndroidNAndAbove()
    }

    override fun getActiveNetworkState(): NetworkState {
        // Use getActiveNetworkInfo() instead of getNetworkInfo(network) because it can detect VPNs.
        val info = mConnectivityManager.activeNetworkInfo
        val isConnected = info != null && info.isConnected
        val isValidated = isActiveNetworkValidated()
        val isMetered: Boolean =
            ConnectivityManagerCompat.isActiveNetworkMetered(mConnectivityManager)
        val isNotRoaming = info != null && !info.isRoaming
        return NetworkState(isConnected, isValidated, isMetered, isNotRoaming, info)
    }

    private fun isActiveNetworkValidated(): Boolean {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            false // NET_CAPABILITY_VALIDATED not available until API 23. Used on API 26+.
        } else try {
            val network: Network? = mConnectivityManager.activeNetwork
            val capabilities =
                mConnectivityManager.getNetworkCapabilities(network)
            (capabilities != null
                    && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
        } catch (exception: SecurityException) {
            // b/163342798
            logger.e("NetworkStateTracker", "Unable to validate active network", exception)
            false
        }
    }

    @RequiresApi(24)
    @VisibleForTesting
    internal inner class NetworkStateCallback : NetworkCallback() {
        override fun onCapabilitiesChanged(
            @NonNull network: Network, @NonNull capabilities: NetworkCapabilities
        ) {
            // The Network parameter is unreliable when a VPN app is running - use active network.
            logger.d(
                "NetworkStateTracker", String.format("Network capabilities changed: %s", capabilities)
            )
            setState(getActiveNetworkState())
        }

        override fun onLost(@NonNull network: Network) {
            logger.d("NetworkStateTracker", "Network connection lost")
            setState(getActiveNetworkState())
        }
    }

    @VisibleForTesting
    internal inner class NetworkStateBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null || intent.action == null) {
                return
            }
            if (intent.action == CONNECTIVITY_ACTION) {
                logger.d("NetworkStateTracker", "Network broadcast received")
                setState(getActiveNetworkState())
            }
        }
    }
}