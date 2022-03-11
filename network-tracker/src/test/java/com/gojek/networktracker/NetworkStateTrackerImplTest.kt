package com.gojek.networktracker

import android.content.Context
import android.content.Context.CONNECTIVITY_SERVICE
import android.content.Intent
import android.net.ConnectivityManager
import android.net.ConnectivityManager.CONNECTIVITY_ACTION
import android.net.ConnectivityManager.NetworkCallback
import com.gojek.courier.logging.ILogger
import com.gojek.networktracker.model.NetworkState
import com.gojek.networktracker.util.BuildInfoProvider
import com.nhaarman.mockitokotlin2.*
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class NetworkStateTrackerImplTest {
    private val context = mock<Context>()
    private val connectivityManager = mock<ConnectivityManager>()
    private val logger = mock<ILogger>()
    private val listener = mock<NetworkStateListener>()
    private val networkState = mock<NetworkState>()
    private val buildInfoProvider = mock<BuildInfoProvider>()

    private lateinit var networkStateTracker: NetworkStateTrackerImpl

    @Before
    fun setup() {
        whenever(context.getSystemService(CONNECTIVITY_SERVICE)).thenReturn(connectivityManager)
        networkStateTracker = spy(NetworkStateTrackerImpl(context, logger, buildInfoProvider))
    }

    @Test
    fun `test addListener for first time should start tracking`() {
        doReturn(networkState).whenever(networkStateTracker).getInitialState()
        doNothing().whenever(networkStateTracker).startTracking()

        networkStateTracker.mListeners.clear()
        networkStateTracker.addListener(listener)

        verify(networkStateTracker).getInitialState()
        verify(networkStateTracker).startTracking()
        verify(listener).onStateChanged(networkState)
    }

    @Test
    fun `test addListener again should not start tracking`() {
        networkStateTracker.mCurrentState = networkState
        networkStateTracker.mListeners.add(listener)

        networkStateTracker.addListener(listener)

        verify(networkStateTracker, times(0)).startTracking()
        verify(listener).onStateChanged(networkState)
    }

    @Test
    fun `test removeListener for last time should stop tracking`() {
        doNothing().whenever(networkStateTracker).stopTracking()
        networkStateTracker.mListeners.add(listener)

        networkStateTracker.removeListener(listener)

        verify(networkStateTracker).stopTracking()
    }

    @Test
    fun `test removeListener with existing listeners should not start tracking`() {
        networkStateTracker.mListeners.add(listener)
        val listener2 = mock<NetworkStateListener>()
        networkStateTracker.mListeners.add(listener2)

        networkStateTracker.removeListener(listener)

        verify(networkStateTracker, times(0)).stopTracking()
    }

    @Test
    fun `test getInitialState should invoke getActiveNetworkState`() {
        doReturn(networkState).whenever(networkStateTracker).getActiveNetworkState()

        val state = networkStateTracker.getInitialState()

        assertEquals(networkState, state)
        verify(networkStateTracker).getActiveNetworkState()
    }

    @Test
    fun `test startTracking when isNetworkCallbackSupported is true`() {
        doReturn(true).whenever(networkStateTracker).isNetworkCallbackSupported()
        networkStateTracker.mNetworkCallback = networkStateTracker.NetworkStateCallback()

        networkStateTracker.startTracking()

        verify(networkStateTracker).isNetworkCallbackSupported()
        verify(connectivityManager).registerDefaultNetworkCallback(any())
    }

    @Test
    fun `test startTracking when isNetworkCallbackSupported is false`() {
        doReturn(false).whenever(networkStateTracker).isNetworkCallbackSupported()
        networkStateTracker.mBroadcastReceiver = networkStateTracker.NetworkStateBroadcastReceiver()

        networkStateTracker.startTracking()

        verify(networkStateTracker).isNetworkCallbackSupported()
        verify(context).registerReceiver(any(), any())
    }

    @Test
    fun `test stopTracking when isNetworkCallbackSupported is true`() {
        doReturn(true).whenever(networkStateTracker).isNetworkCallbackSupported()
        networkStateTracker.mNetworkCallback = networkStateTracker.NetworkStateCallback()

        networkStateTracker.stopTracking()

        verify(networkStateTracker).isNetworkCallbackSupported()
        verify(connectivityManager).unregisterNetworkCallback(any<NetworkCallback>())
    }

    @Test
    fun `test stopTracking when isNetworkCallbackSupported is false`() {
        doReturn(false).whenever(networkStateTracker).isNetworkCallbackSupported()
        networkStateTracker.mBroadcastReceiver = networkStateTracker.NetworkStateBroadcastReceiver()

        networkStateTracker.stopTracking()

        verify(networkStateTracker).isNetworkCallbackSupported()
        verify(context).unregisterReceiver(any())
    }

    @Test
    fun `test isNetworkCallbackSupported should invoke buildInfoProvider`() {
        networkStateTracker.isNetworkCallbackSupported()

        //Invoked once in init block
        verify(buildInfoProvider, times(2)).isAndroidNAndAbove()
    }
}