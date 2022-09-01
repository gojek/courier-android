package com.gojek.keepalive

import android.net.NetworkInfo
import com.gojek.keepalive.utils.NetworkUtils
import com.gojek.mqtt.pingsender.KeepAlive
import com.gojek.networktracker.NetworkStateTracker
import com.gojek.networktracker.model.NetworkState
import com.nhaarman.mockitokotlin2.doNothing
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import kotlin.test.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class OptimalKeepAliveCalculatorTest {
    private val networkTracker = mock<NetworkStateTracker>()
    private val networkUtils = mock<NetworkUtils>()
    private val stateHandler = mock<AdaptiveKeepAliveStateHandler>()
    private val optimalKeepAliveObserver = mock<OptimalKeepAliveObserver>()

    private val optimalKeepAliveCalculator = OptimalKeepAliveCalculator(
        networkTracker = networkTracker,
        networkUtils = networkUtils,
        optimalKeepAliveObserver = optimalKeepAliveObserver,
        stateHandler = stateHandler
    )

    @Before
    fun setup() {
        verify(networkTracker).addListener(optimalKeepAliveCalculator.networkStateListener)
    }

    @Test
    fun `test onStateChanged should notify state handler when network is connected`() {
        val networkState = mock<NetworkState>()
        val netInfo = mock<NetworkInfo>()
        whenever(networkState.isConnected).thenReturn(true)
        whenever(networkState.netInfo).thenReturn(netInfo)
        val networkType = 1
        val networkName = "test-network"
        whenever(networkUtils.getNetworkType(netInfo)).thenReturn(networkType)
        whenever(networkUtils.getNetworkName(netInfo)).thenReturn(networkName)

        optimalKeepAliveCalculator.networkStateListener.onStateChanged(networkState)

        verify(stateHandler).onNetworkChanged(networkType, networkName)
        verify(networkState).isConnected
        verify(networkState, times(2)).netInfo
        verify(networkUtils).getNetworkType(netInfo)
        verify(networkUtils).getNetworkName(netInfo)
        verifyNoMoreInteractions(networkState)
    }

    @Test
    fun `test onStateChanged should not notify state handler when network is not connected`() {
        val networkState = mock<NetworkState>()
        whenever(networkState.isConnected).thenReturn(false)

        optimalKeepAliveCalculator.networkStateListener.onStateChanged(networkState)

        verify(networkState).isConnected
    }

    @Test
    fun `test getUnderTrialKeepAlive when optimal keep alive is already found`() {
        val optimalKeepAlive = mock<KeepAlive>()
        whenever(stateHandler.isOptimalKeepAliveFound()).thenReturn(true)
        whenever(stateHandler.getOptimalKeepAlive()).thenReturn(optimalKeepAlive)

        val underTrialKeepAlive = optimalKeepAliveCalculator.getUnderTrialKeepAlive()

        assertEquals(optimalKeepAlive, underTrialKeepAlive)
        verify(stateHandler).isOptimalKeepAliveFound()
        verify(stateHandler).getOptimalKeepAlive()
    }

    @Test
    fun `test when optimal KA is not found and current KA failure limit is not exceeded`() {
        val keepAlive = mock<KeepAlive>()
        whenever(stateHandler.isOptimalKeepAliveFound()).thenReturn(false)
        whenever(stateHandler.isCurrentKeepAliveFailureLimitExceeded()).thenReturn(false)
        whenever(stateHandler.getCurrentKeepAlive()).thenReturn(keepAlive)

        val underTrialKeepAlive = optimalKeepAliveCalculator.getUnderTrialKeepAlive()

        assertEquals(keepAlive, underTrialKeepAlive)
        verify(stateHandler).isOptimalKeepAliveFound()
        verify(stateHandler).calculateNextKeepAlive()
        verify(stateHandler).isCurrentKeepAliveFailureLimitExceeded()
        verify(stateHandler).updateProbeCountAndConvergenceTime()
        verify(stateHandler).updatePersistenceWithLatestState()
        verify(stateHandler).getCurrentKeepAlive()
    }

    @Test
    fun `test when optimal KA is not found and current KA failure limit is exceeded`() {
        val keepAlive = mock<KeepAlive>()
        val optimalKeepAlive = mock<KeepAlive>()
        whenever(stateHandler.isOptimalKeepAliveFound()).thenReturn(false, true)
        whenever(stateHandler.getOptimalKeepAlive()).thenReturn(optimalKeepAlive)
        whenever(stateHandler.isCurrentKeepAliveFailureLimitExceeded()).thenReturn(true)
        whenever(stateHandler.getCurrentKeepAlive()).thenReturn(keepAlive)

        val optimalKeepAliveCalculatorSpy = spy(optimalKeepAliveCalculator)
        doNothing().whenever(optimalKeepAliveCalculatorSpy).handleKeepAliveFailure(keepAlive)

        val underTrialKeepAlive = optimalKeepAliveCalculatorSpy.getUnderTrialKeepAlive()

        assertEquals(optimalKeepAlive, underTrialKeepAlive)
        verify(stateHandler, times(2)).isOptimalKeepAliveFound()
        verify(stateHandler).calculateNextKeepAlive()
        verify(stateHandler).isCurrentKeepAliveFailureLimitExceeded()
        verify(stateHandler).getCurrentKeepAlive()
        verify(stateHandler).getOptimalKeepAlive()
        verify(optimalKeepAliveCalculatorSpy).handleKeepAliveFailure(keepAlive)
    }

    @Test
    fun `test onKeepAliveSuccess when keep alive succeeded is invalid`() {
        val keepAlive = mock<KeepAlive>()
        whenever(stateHandler.isValidKeepAlive(keepAlive)).thenReturn(false)

        optimalKeepAliveCalculator.onKeepAliveSuccess(keepAlive)

        verify(stateHandler).isValidKeepAlive(keepAlive)
    }

    @Test
    fun `test when keep alive succeeded is valid and optimal keep alive is found`() {
        val keepAlive = mock<KeepAlive>()
        val optimalKeepAlive = KeepAlive(1, "test-network", 5)
        val probeCount = 7
        val convergenceTime = 29
        whenever(stateHandler.isValidKeepAlive(keepAlive)).thenReturn(true)
        whenever(stateHandler.isOptimalKeepAliveFound()).thenReturn(true)
        whenever(stateHandler.getOptimalKeepAlive()).thenReturn(optimalKeepAlive)
        whenever(stateHandler.getProbeCount()).thenReturn(probeCount)
        whenever(stateHandler.getConvergenceTime()).thenReturn(convergenceTime)

        optimalKeepAliveCalculator.onKeepAliveSuccess(keepAlive)

        verify(stateHandler).isValidKeepAlive(keepAlive)
        verify(stateHandler).updateKeepAliveSuccessState(keepAlive)
        verify(stateHandler).isOptimalKeepAliveFound()
        verify(stateHandler).getOptimalKeepAlive()
        verify(stateHandler).getProbeCount()
        verify(stateHandler).getConvergenceTime()
        verify(optimalKeepAliveObserver).onOptimalKeepAliveFound(
            timeMinutes = optimalKeepAlive.keepAliveMinutes,
            probeCount = probeCount,
            convergenceTime = convergenceTime
        )
        verify(stateHandler).updatePersistenceWithLatestState()
    }

    @Test
    fun `test when keep alive succeeded is valid and optimal keep alive is not found`() {
        val keepAlive = mock<KeepAlive>()
        whenever(stateHandler.isValidKeepAlive(keepAlive)).thenReturn(true)
        whenever(stateHandler.isOptimalKeepAliveFound()).thenReturn(false)

        optimalKeepAliveCalculator.onKeepAliveSuccess(keepAlive)

        verify(stateHandler).isValidKeepAlive(keepAlive)
        verify(stateHandler).updateKeepAliveSuccessState(keepAlive)
        verify(stateHandler).isOptimalKeepAliveFound()
        verify(stateHandler).updatePersistenceWithLatestState()
    }

    @Test
    fun `test onKeepAliveFailure when keep alive failed is invalid`() {
        val keepAlive = mock<KeepAlive>()
        whenever(stateHandler.isValidKeepAlive(keepAlive)).thenReturn(false)

        optimalKeepAliveCalculator.onKeepAliveFailure(keepAlive)

        verify(stateHandler).isValidKeepAlive(keepAlive)
    }

    @Test
    fun `test when keep alive failed is valid and optimal keep alive is found`() {
        val keepAlive = mock<KeepAlive>()
        val optimalKeepAlive = KeepAlive(1, "test-network", 5)
        val probeCount = 7
        val convergenceTime = 29
        whenever(stateHandler.isValidKeepAlive(keepAlive)).thenReturn(true)
        whenever(stateHandler.isOptimalKeepAliveFound()).thenReturn(true)
        whenever(stateHandler.getOptimalKeepAlive()).thenReturn(optimalKeepAlive)
        whenever(stateHandler.getProbeCount()).thenReturn(probeCount)
        whenever(stateHandler.getConvergenceTime()).thenReturn(convergenceTime)

        optimalKeepAliveCalculator.onKeepAliveFailure(keepAlive)

        verify(stateHandler).isValidKeepAlive(keepAlive)
        verify(stateHandler).updateKeepAliveFailureState(keepAlive)
        verify(stateHandler).isOptimalKeepAliveFound()
        verify(stateHandler).getOptimalKeepAlive()
        verify(stateHandler).getProbeCount()
        verify(stateHandler).getConvergenceTime()
        verify(optimalKeepAliveObserver).onOptimalKeepAliveFound(
            timeMinutes = optimalKeepAlive.keepAliveMinutes,
            probeCount = probeCount,
            convergenceTime = convergenceTime
        )
        verify(stateHandler).updatePersistenceWithLatestState()
    }

    @Test
    fun `test when keep alive failed is valid and optimal keep alive is not found`() {
        val keepAlive = mock<KeepAlive>()
        whenever(stateHandler.isValidKeepAlive(keepAlive)).thenReturn(true)
        whenever(stateHandler.isOptimalKeepAliveFound()).thenReturn(false)

        optimalKeepAliveCalculator.onKeepAliveFailure(keepAlive)

        verify(stateHandler).isValidKeepAlive(keepAlive)
        verify(stateHandler).updateKeepAliveFailureState(keepAlive)
        verify(stateHandler).isOptimalKeepAliveFound()
        verify(stateHandler).updatePersistenceWithLatestState()
    }

    @Test
    fun `test getOptimalKeepAlive when optimal keep alive is not found`() {
        whenever(stateHandler.isOptimalKeepAliveFound()).thenReturn(false)

        assertEquals(0, optimalKeepAliveCalculator.getOptimalKeepAlive())

        verify(stateHandler).isOptimalKeepAliveFound()
    }

    @Test
    fun `test getOptimalKeepAlive when optimal keep alive is found`() {
        val optimalKeepAlive = KeepAlive(1, "test-network", 5)
        whenever(stateHandler.isOptimalKeepAliveFound()).thenReturn(true)
        whenever(stateHandler.getOptimalKeepAlive()).thenReturn(optimalKeepAlive)

        assertEquals(
            optimalKeepAlive.keepAliveMinutes,
            optimalKeepAliveCalculator.getOptimalKeepAlive()
        )

        verify(stateHandler).isOptimalKeepAliveFound()
        verify(stateHandler).getOptimalKeepAlive()
    }

    @Test
    fun `test onOptimalKeepAliveFailure when optimal keep alive is not found`() {
        whenever(stateHandler.isOptimalKeepAliveFound()).thenReturn(false)

        optimalKeepAliveCalculator.onOptimalKeepAliveFailure()

        verify(stateHandler).isOptimalKeepAliveFound()
    }

    @Test
    fun `test when optimal keep alive is found and failure limit is exceeded`() {
        whenever(stateHandler.isOptimalKeepAliveFound()).thenReturn(true)
        whenever(stateHandler.isOptimalKeepAliveFailureLimitExceeded()).thenReturn(true)

        optimalKeepAliveCalculator.onOptimalKeepAliveFailure()

        verify(stateHandler).isOptimalKeepAliveFound()
        verify(stateHandler).updateOptimalKeepAliveFailureState()
        verify(stateHandler).isOptimalKeepAliveFailureLimitExceeded()
        verify(stateHandler).removeStateFromPersistence()
    }

    @Test
    fun `test when optimal keep alive is found and failure limit is not exceeded`() {
        whenever(stateHandler.isOptimalKeepAliveFound()).thenReturn(true)
        whenever(stateHandler.isOptimalKeepAliveFailureLimitExceeded()).thenReturn(false)

        optimalKeepAliveCalculator.onOptimalKeepAliveFailure()

        verify(stateHandler).isOptimalKeepAliveFound()
        verify(stateHandler).updateOptimalKeepAliveFailureState()
        verify(stateHandler).isOptimalKeepAliveFailureLimitExceeded()
    }

    @After
    fun teardown() {
        verifyNoMoreInteractions(
            stateHandler,
            networkTracker,
            networkUtils,
            optimalKeepAliveObserver
        )
    }
}
