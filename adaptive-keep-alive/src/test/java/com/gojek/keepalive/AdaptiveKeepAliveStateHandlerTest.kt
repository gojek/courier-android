package com.gojek.keepalive

import com.gojek.keepalive.AdaptiveKeepAliveStateHandler.Companion.MAX_CURRENT_KEEPALIVE_FAILURE
import com.gojek.keepalive.model.KeepAlivePersistenceModel
import com.gojek.keepalive.persistence.KeepAlivePersistence
import com.gojek.mqtt.pingsender.KeepAlive
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class AdaptiveKeepAliveStateHandlerTest {

    private val lowerBound: Int = 2
    private val upperBound: Int = 10
    private val step: Int = 2
    private val optimalKeepAliveResetLimit: Int = 30
    private val keepAlivePersistence = mock<KeepAlivePersistence>()

    private val adaptiveKeepAliveStateHandler = AdaptiveKeepAliveStateHandler(
        lowerBound = lowerBound,
        upperBound = upperBound,
        step = step,
        optimalKeepAliveResetLimit = optimalKeepAliveResetLimit,
        persistence = keepAlivePersistence
    )

    @Test
    fun `test initial state`() {
        adaptiveKeepAliveStateHandler.let {
            assertEquals(lowerBound - step, it.state.lastSuccessfulKA)
            assertEquals(false, it.state.isOptimalKeepAlive)
            assertEquals(upperBound, it.state.currentUpperBound)
            assertEquals(step, it.state.currentStep)
            assertEquals(-1, it.state.currentKA)
            assertEquals(0, it.state.currentKAFailureCount)
            assertEquals(0, it.state.probeCount)
            assertEquals(0, it.state.convergenceTime)
            assertEquals(0, it.state.optimalKAFailureCount)
            assertEquals(-1, it.state.currentNetworkType)
            assertEquals("", it.state.currentNetworkName)
            assertEquals(lowerBound, it.state.lowerBound)
            assertEquals(upperBound, it.state.upperBound)
            assertEquals(step, it.state.step)
            assertEquals(optimalKeepAliveResetLimit, it.state.optimalKeepAliveResetLimit)
        }
    }

    @Test
    fun `test onNetworkChanged with network info same as current network info`() {
        setCurrentNetworkInfo()

        val oldKeepAlive = adaptiveKeepAliveStateHandler.getCurrentKeepAlive()
        adaptiveKeepAliveStateHandler.onNetworkChanged(CURRENT_NETWORK_TYPE, CURRENT_NETWORK)
        val newKeepAlive = adaptiveKeepAliveStateHandler.getCurrentKeepAlive()

        assertEquals(oldKeepAlive, newKeepAlive)
    }

    @Test
    fun `test onNetworkChanged with network info different from current network info and no info available in persistence`() {
        setOldNetworkInfo()
        whenever(keepAlivePersistence.has(getCurrentNetworkKey())).thenReturn(false)

        val oldKeepAlive = adaptiveKeepAliveStateHandler.getCurrentKeepAlive()
        adaptiveKeepAliveStateHandler.onNetworkChanged(CURRENT_NETWORK_TYPE, CURRENT_NETWORK)
        val newKeepAlive = adaptiveKeepAliveStateHandler.getCurrentKeepAlive()

        assertNotEquals(oldKeepAlive, newKeepAlive)
        verifyCurrentNetworkInfo()
        verify(keepAlivePersistence).has(getCurrentNetworkKey())
    }

    @Test
    fun `test onNetworkChanged with network info different from current network info and keep alive info available in persistence`() {
        setOldNetworkInfo()
        whenever(keepAlivePersistence.has(getCurrentNetworkKey())).thenReturn(true)
        whenever(keepAlivePersistence.get(getCurrentNetworkKey()))
            .thenReturn(getKeepAlivePersistenceModel(lowerBound, upperBound))

        val oldKeepAlive = adaptiveKeepAliveStateHandler.getCurrentKeepAlive()
        adaptiveKeepAliveStateHandler.onNetworkChanged(CURRENT_NETWORK_TYPE, CURRENT_NETWORK)
        val newKeepAlive = adaptiveKeepAliveStateHandler.getCurrentKeepAlive()

        assertNotEquals(oldKeepAlive, newKeepAlive)
        assertTrue(adaptiveKeepAliveStateHandler.state.isOptimalKeepAlive)
        verifyCurrentNetworkInfo()
        verify(keepAlivePersistence).has(getCurrentNetworkKey())
        verify(keepAlivePersistence).get(getCurrentNetworkKey())
    }

    @Test
    fun `test onNetworkChanged with network info different from current network info and keepalive info available in persistence with different upper bound`() {
        setOldNetworkInfo()
        whenever(keepAlivePersistence.has(getCurrentNetworkKey())).thenReturn(true)
        whenever(keepAlivePersistence.get(getCurrentNetworkKey()))
            .thenReturn(getKeepAlivePersistenceModel(lowerBound, upperBound+1))

        val oldKeepAlive = adaptiveKeepAliveStateHandler.getCurrentKeepAlive()
        adaptiveKeepAliveStateHandler.onNetworkChanged(CURRENT_NETWORK_TYPE, CURRENT_NETWORK)
        val newKeepAlive = adaptiveKeepAliveStateHandler.getCurrentKeepAlive()

        assertNotEquals(oldKeepAlive, newKeepAlive)
        assertFalse(adaptiveKeepAliveStateHandler.state.isOptimalKeepAlive)
        verifyCurrentNetworkInfo()
        verify(keepAlivePersistence).has(getCurrentNetworkKey())
        verify(keepAlivePersistence).get(getCurrentNetworkKey())
    }

    @Test
    fun `test onNetworkChanged with network info different from current network info and keepalive info available in persistence with different lower bound`() {
        setOldNetworkInfo()
        whenever(keepAlivePersistence.has(getCurrentNetworkKey())).thenReturn(true)
        whenever(keepAlivePersistence.get(getCurrentNetworkKey()))
            .thenReturn(getKeepAlivePersistenceModel(lowerBound+1, upperBound))

        val oldKeepAlive = adaptiveKeepAliveStateHandler.getCurrentKeepAlive()
        adaptiveKeepAliveStateHandler.onNetworkChanged(CURRENT_NETWORK_TYPE, CURRENT_NETWORK)
        val newKeepAlive = adaptiveKeepAliveStateHandler.getCurrentKeepAlive()

        assertNotEquals(oldKeepAlive, newKeepAlive)
        assertFalse(adaptiveKeepAliveStateHandler.state.isOptimalKeepAlive)
        verifyCurrentNetworkInfo()
        verify(keepAlivePersistence).has(getCurrentNetworkKey())
        verify(keepAlivePersistence).get(getCurrentNetworkKey())
    }

    @Test
    fun `test updateKeepAliveSuccessState when keep alive is equal to current upper bound`() {
        val keepAlive = mock<KeepAlive>()
        whenever(keepAlive.keepAliveMinutes)
            .thenReturn(adaptiveKeepAliveStateHandler.state.currentUpperBound)

        val oldState = adaptiveKeepAliveStateHandler.state
        adaptiveKeepAliveStateHandler.updateKeepAliveSuccessState(keepAlive)
        val newState = adaptiveKeepAliveStateHandler.state

        assertEquals(oldState.currentUpperBound, newState.lastSuccessfulKA)
        assertEquals(true, newState.isOptimalKeepAlive)
        assertEquals(oldState.currentUpperBound, newState.currentUpperBound)
        assertEquals(oldState.currentStep, newState.currentStep)
        assertEquals(oldState.currentKA, newState.currentKA)
        assertEquals(0, newState.currentKAFailureCount)
        assertEquals(oldState.probeCount, newState.probeCount)
        assertEquals(oldState.convergenceTime, newState.convergenceTime)
        assertEquals(oldState.optimalKAFailureCount, newState.optimalKAFailureCount)
        assertEquals(oldState.currentNetworkType, newState.currentNetworkType)
        assertEquals(oldState.currentNetworkName, newState.currentNetworkName)
        assertEquals(oldState.lowerBound, newState.lowerBound)
        assertEquals(oldState.upperBound, newState.upperBound)
        assertEquals(oldState.step, newState.step)
        assertEquals(oldState.optimalKeepAliveResetLimit, newState.optimalKeepAliveResetLimit)
    }

    @Test
    fun `test updateKeepAliveSuccessState when keep alive is not equal to current upper bound`() {
        val keepAlive = mock<KeepAlive>()
        whenever(keepAlive.keepAliveMinutes)
            .thenReturn(adaptiveKeepAliveStateHandler.state.currentUpperBound - 1)

        val oldState = adaptiveKeepAliveStateHandler.state
        adaptiveKeepAliveStateHandler.updateKeepAliveSuccessState(keepAlive)
        val newState = adaptiveKeepAliveStateHandler.state

        assertEquals(oldState.currentUpperBound - 1, newState.lastSuccessfulKA)
        assertEquals(oldState.isOptimalKeepAlive, newState.isOptimalKeepAlive)
        assertEquals(oldState.currentUpperBound, newState.currentUpperBound)
        assertEquals(oldState.currentStep, newState.currentStep)
        assertEquals(oldState.currentKA, newState.currentKA)
        assertEquals(0, newState.currentKAFailureCount)
        assertEquals(oldState.probeCount, newState.probeCount)
        assertEquals(oldState.convergenceTime, newState.convergenceTime)
        assertEquals(oldState.optimalKAFailureCount, newState.optimalKAFailureCount)
        assertEquals(oldState.currentNetworkType, newState.currentNetworkType)
        assertEquals(oldState.currentNetworkName, newState.currentNetworkName)
        assertEquals(oldState.lowerBound, newState.lowerBound)
        assertEquals(oldState.upperBound, newState.upperBound)
        assertEquals(oldState.step, newState.step)
        assertEquals(oldState.optimalKeepAliveResetLimit, newState.optimalKeepAliveResetLimit)
    }

    @Test
    fun `test updateKeepAliveFailureState when keep alive fails for lower bound`() {
        val keepAlive = mock<KeepAlive>()
        whenever(keepAlive.keepAliveMinutes)
            .thenReturn(adaptiveKeepAliveStateHandler.state.lowerBound)

        val oldState = adaptiveKeepAliveStateHandler.state
        adaptiveKeepAliveStateHandler.updateKeepAliveFailureState(keepAlive)
        val newState = adaptiveKeepAliveStateHandler.state

        assertEquals(oldState.lowerBound, newState.lastSuccessfulKA)
        assertEquals(true, newState.isOptimalKeepAlive)
        assertEquals(oldState.currentUpperBound, newState.currentUpperBound)
        assertEquals(oldState.currentStep, newState.currentStep)
        assertEquals(oldState.currentKA, newState.currentKA)
        assertEquals(0, newState.currentKAFailureCount)
        assertEquals(oldState.probeCount, newState.probeCount)
        assertEquals(oldState.convergenceTime, newState.convergenceTime)
        assertEquals(oldState.optimalKAFailureCount, newState.optimalKAFailureCount)
        assertEquals(oldState.currentNetworkType, newState.currentNetworkType)
        assertEquals(oldState.currentNetworkName, newState.currentNetworkName)
        assertEquals(oldState.lowerBound, newState.lowerBound)
        assertEquals(oldState.upperBound, newState.upperBound)
        assertEquals(oldState.step, newState.step)
        assertEquals(oldState.optimalKeepAliveResetLimit, newState.optimalKeepAliveResetLimit)
    }

    @Test
    fun `test updateKeepAliveFailureState when keep alive fails and new upper bound is same as last successful`() {
        adaptiveKeepAliveStateHandler.state = adaptiveKeepAliveStateHandler.state.copy(
            currentUpperBound = adaptiveKeepAliveStateHandler.state.lastSuccessfulKA + 1
        )
        val keepAlive = mock<KeepAlive>()
        whenever(keepAlive.keepAliveMinutes)
            .thenReturn(adaptiveKeepAliveStateHandler.state.currentUpperBound)

        val oldState = adaptiveKeepAliveStateHandler.state
        adaptiveKeepAliveStateHandler.updateKeepAliveFailureState(keepAlive)
        val newState = adaptiveKeepAliveStateHandler.state

        assertEquals(oldState.lastSuccessfulKA, newState.lastSuccessfulKA)
        assertEquals(true, newState.isOptimalKeepAlive)
        assertEquals(oldState.currentUpperBound - 1, newState.currentUpperBound)
        assertEquals(oldState.currentStep, newState.currentStep)
        assertEquals(oldState.currentKA, newState.currentKA)
        assertEquals(0, newState.currentKAFailureCount)
        assertEquals(oldState.probeCount, newState.probeCount)
        assertEquals(oldState.convergenceTime, newState.convergenceTime)
        assertEquals(oldState.optimalKAFailureCount, newState.optimalKAFailureCount)
        assertEquals(oldState.currentNetworkType, newState.currentNetworkType)
        assertEquals(oldState.currentNetworkName, newState.currentNetworkName)
        assertEquals(oldState.lowerBound, newState.lowerBound)
        assertEquals(oldState.upperBound, newState.upperBound)
        assertEquals(oldState.step, newState.step)
        assertEquals(oldState.optimalKeepAliveResetLimit, newState.optimalKeepAliveResetLimit)
    }

    @Test
    fun `test updateKeepAliveFailureState when keep alive fails`() {
        adaptiveKeepAliveStateHandler.state = adaptiveKeepAliveStateHandler.state.copy(
            currentUpperBound = adaptiveKeepAliveStateHandler.state.lastSuccessfulKA + 5
        )
        val keepAlive = mock<KeepAlive>()
        whenever(keepAlive.keepAliveMinutes)
            .thenReturn(adaptiveKeepAliveStateHandler.state.currentUpperBound)

        val oldState = adaptiveKeepAliveStateHandler.state
        adaptiveKeepAliveStateHandler.updateKeepAliveFailureState(keepAlive)
        val newState = adaptiveKeepAliveStateHandler.state

        assertEquals(oldState.lastSuccessfulKA, newState.lastSuccessfulKA)
        assertEquals(oldState.isOptimalKeepAlive, newState.isOptimalKeepAlive)
        assertEquals(oldState.currentUpperBound - 1, newState.currentUpperBound)
        assertEquals(oldState.currentStep / 2, newState.currentStep)
        assertEquals(oldState.currentKA, newState.currentKA)
        assertEquals(0, newState.currentKAFailureCount)
        assertEquals(oldState.probeCount, newState.probeCount)
        assertEquals(oldState.convergenceTime, newState.convergenceTime)
        assertEquals(oldState.optimalKAFailureCount, newState.optimalKAFailureCount)
        assertEquals(oldState.currentNetworkType, newState.currentNetworkType)
        assertEquals(oldState.currentNetworkName, newState.currentNetworkName)
        assertEquals(oldState.lowerBound, newState.lowerBound)
        assertEquals(oldState.upperBound, newState.upperBound)
        assertEquals(oldState.step, newState.step)
        assertEquals(oldState.optimalKeepAliveResetLimit, newState.optimalKeepAliveResetLimit)
    }

    @Test
    fun `test calculateNextKeepAlive when current keepalive is already tried`() {
        with(adaptiveKeepAliveStateHandler) {
            state = state.copy(
                lastSuccessfulKA = 2,
                currentStep = 2,
                currentKA = 4,
                currentUpperBound = 5,
            )
        }

        val oldState = adaptiveKeepAliveStateHandler.state
        adaptiveKeepAliveStateHandler.calculateNextKeepAlive()
        val newState = adaptiveKeepAliveStateHandler.state

        assertEquals(oldState.lastSuccessfulKA, newState.lastSuccessfulKA)
        assertEquals(oldState.isOptimalKeepAlive, newState.isOptimalKeepAlive)
        assertEquals(oldState.currentUpperBound, newState.currentUpperBound)
        assertEquals(oldState.currentStep, newState.currentStep)
        assertEquals(oldState.currentKA, newState.currentKA)
        assertEquals(oldState.currentKAFailureCount + 1, newState.currentKAFailureCount)
        assertEquals(oldState.probeCount, newState.probeCount)
        assertEquals(oldState.convergenceTime, newState.convergenceTime)
        assertEquals(oldState.optimalKAFailureCount, newState.optimalKAFailureCount)
        assertEquals(oldState.currentNetworkType, newState.currentNetworkType)
        assertEquals(oldState.currentNetworkName, newState.currentNetworkName)
        assertEquals(oldState.lowerBound, newState.lowerBound)
        assertEquals(oldState.upperBound, newState.upperBound)
        assertEquals(oldState.step, newState.step)
        assertEquals(oldState.optimalKeepAliveResetLimit, newState.optimalKeepAliveResetLimit)
    }

    @Test
    fun `test calculateNextKeepAlive when current keepalive is not already tried`() {
        with(adaptiveKeepAliveStateHandler) {
            state = state.copy(
                lastSuccessfulKA = 2,
                currentStep = 2,
                currentKA = 2,
                currentUpperBound = 5,
            )
        }

        val oldState = adaptiveKeepAliveStateHandler.state
        adaptiveKeepAliveStateHandler.calculateNextKeepAlive()
        val newState = adaptiveKeepAliveStateHandler.state

        assertEquals(oldState.lastSuccessfulKA, newState.lastSuccessfulKA)
        assertEquals(oldState.isOptimalKeepAlive, newState.isOptimalKeepAlive)
        assertEquals(oldState.currentUpperBound, newState.currentUpperBound)
        assertEquals(oldState.currentStep, newState.currentStep)
        assertEquals(oldState.lastSuccessfulKA + oldState.currentStep, newState.currentKA)
        assertEquals(0, newState.currentKAFailureCount)
        assertEquals(oldState.probeCount, newState.probeCount)
        assertEquals(oldState.convergenceTime, newState.convergenceTime)
        assertEquals(oldState.optimalKAFailureCount, newState.optimalKAFailureCount)
        assertEquals(oldState.currentNetworkType, newState.currentNetworkType)
        assertEquals(oldState.currentNetworkName, newState.currentNetworkName)
        assertEquals(oldState.lowerBound, newState.lowerBound)
        assertEquals(oldState.upperBound, newState.upperBound)
        assertEquals(oldState.step, newState.step)
        assertEquals(oldState.optimalKeepAliveResetLimit, newState.optimalKeepAliveResetLimit)
    }

    @Test
    fun `test calculateNextKeepAlive when upper bound is less than next keep alive`() {
        with(adaptiveKeepAliveStateHandler) {
            state = state.copy(
                lastSuccessfulKA = 2,
                currentStep = 2,
                currentKA = 2,
                currentUpperBound = 3,
            )
        }

        val oldState = adaptiveKeepAliveStateHandler.state
        adaptiveKeepAliveStateHandler.calculateNextKeepAlive()
        val newState = adaptiveKeepAliveStateHandler.state

        assertEquals(oldState.lastSuccessfulKA, newState.lastSuccessfulKA)
        assertEquals(oldState.isOptimalKeepAlive, newState.isOptimalKeepAlive)
        assertEquals(oldState.currentUpperBound, newState.currentUpperBound)
        assertEquals(oldState.currentStep, newState.currentStep)
        assertEquals(oldState.currentUpperBound, newState.currentKA)
        assertEquals(0, newState.currentKAFailureCount)
        assertEquals(oldState.probeCount, newState.probeCount)
        assertEquals(oldState.convergenceTime, newState.convergenceTime)
        assertEquals(oldState.optimalKAFailureCount, newState.optimalKAFailureCount)
        assertEquals(oldState.currentNetworkType, newState.currentNetworkType)
        assertEquals(oldState.currentNetworkName, newState.currentNetworkName)
        assertEquals(oldState.lowerBound, newState.lowerBound)
        assertEquals(oldState.upperBound, newState.upperBound)
        assertEquals(oldState.step, newState.step)
        assertEquals(oldState.optimalKeepAliveResetLimit, newState.optimalKeepAliveResetLimit)
    }

    @Test
    fun `test updateOptimalKeepAliveFailureState`() {
        val oldState = adaptiveKeepAliveStateHandler.state
        adaptiveKeepAliveStateHandler.updateOptimalKeepAliveFailureState()
        val newState = adaptiveKeepAliveStateHandler.state

        assertEquals(oldState.lastSuccessfulKA, newState.lastSuccessfulKA)
        assertEquals(oldState.isOptimalKeepAlive, newState.isOptimalKeepAlive)
        assertEquals(oldState.currentUpperBound, newState.currentUpperBound)
        assertEquals(oldState.currentStep, newState.currentStep)
        assertEquals(oldState.currentKA, newState.currentKA)
        assertEquals(oldState.currentKAFailureCount, newState.currentKAFailureCount)
        assertEquals(oldState.probeCount, newState.probeCount)
        assertEquals(oldState.convergenceTime, newState.convergenceTime)
        assertEquals(oldState.optimalKAFailureCount + 1, newState.optimalKAFailureCount)
        assertEquals(oldState.currentNetworkType, newState.currentNetworkType)
        assertEquals(oldState.currentNetworkName, newState.currentNetworkName)
        assertEquals(oldState.lowerBound, newState.lowerBound)
        assertEquals(oldState.upperBound, newState.upperBound)
        assertEquals(oldState.step, newState.step)
        assertEquals(oldState.optimalKeepAliveResetLimit, newState.optimalKeepAliveResetLimit)
    }

    @Test
    fun `test updateProbeCountAndConvergenceTime`() {
        val oldState = adaptiveKeepAliveStateHandler.state
        adaptiveKeepAliveStateHandler.updateProbeCountAndConvergenceTime()
        val newState = adaptiveKeepAliveStateHandler.state

        assertEquals(oldState.lastSuccessfulKA, newState.lastSuccessfulKA)
        assertEquals(oldState.isOptimalKeepAlive, newState.isOptimalKeepAlive)
        assertEquals(oldState.currentUpperBound, newState.currentUpperBound)
        assertEquals(oldState.currentStep, newState.currentStep)
        assertEquals(oldState.currentKA, newState.currentKA)
        assertEquals(oldState.currentKAFailureCount, newState.currentKAFailureCount)
        assertEquals(oldState.probeCount + 1, newState.probeCount)
        assertEquals(oldState.convergenceTime + oldState.currentKA, newState.convergenceTime)
        assertEquals(oldState.optimalKAFailureCount, newState.optimalKAFailureCount)
        assertEquals(oldState.currentNetworkType, newState.currentNetworkType)
        assertEquals(oldState.currentNetworkName, newState.currentNetworkName)
        assertEquals(oldState.lowerBound, newState.lowerBound)
        assertEquals(oldState.upperBound, newState.upperBound)
        assertEquals(oldState.step, newState.step)
        assertEquals(oldState.optimalKeepAliveResetLimit, newState.optimalKeepAliveResetLimit)
    }

    @Test
    fun `test updatePersistenceWithLatestState`() {
        setCurrentNetworkInfo()
        val oldState = adaptiveKeepAliveStateHandler.state
        adaptiveKeepAliveStateHandler.updatePersistenceWithLatestState()
        val newState = adaptiveKeepAliveStateHandler.state

        verify(keepAlivePersistence).put(getCurrentNetworkKey(), getKeepAlivePersistenceModel(newState))
        verifyStates(oldState, newState)
    }

    @Test
    fun `test removeStateFromPersistence`() {
        setCurrentNetworkInfo()
        val oldState = adaptiveKeepAliveStateHandler.state
        adaptiveKeepAliveStateHandler.removeStateFromPersistence()
        val newState = adaptiveKeepAliveStateHandler.state

        verify(keepAlivePersistence).remove(getCurrentNetworkKey())
        verifyStates(oldState, newState)
    }

    @Test
    fun `test getCurrentKeepAlive`() {
        val oldState = adaptiveKeepAliveStateHandler.state
        val currentKeepAlive = adaptiveKeepAliveStateHandler.getCurrentKeepAlive()
        val newState = adaptiveKeepAliveStateHandler.state

        assertEquals(newState.currentKA, currentKeepAlive.keepAliveMinutes)
        assertEquals(newState.currentNetworkType, currentKeepAlive.networkType)
        assertEquals(newState.currentNetworkName, currentKeepAlive.networkName)
        verifyStates(oldState, newState)
    }

    @Test
    fun `test getOptimalKeepAlive`() {
        val oldState = adaptiveKeepAliveStateHandler.state
        val optimalKeepAlive = adaptiveKeepAliveStateHandler.getOptimalKeepAlive()
        val newState = adaptiveKeepAliveStateHandler.state

        assertEquals(newState.lastSuccessfulKA, optimalKeepAlive.keepAliveMinutes)
        assertEquals(newState.currentNetworkType, optimalKeepAlive.networkType)
        assertEquals(newState.currentNetworkName, optimalKeepAlive.networkName)
        verifyStates(oldState, newState)
    }

    @Test
    fun `test isCurrentKeepAliveFailureLimitExceeded should return false when limit is not exceeded`() {
        adaptiveKeepAliveStateHandler.state = adaptiveKeepAliveStateHandler.state.copy(
            currentKAFailureCount = MAX_CURRENT_KEEPALIVE_FAILURE - 1
        )
        val oldState = adaptiveKeepAliveStateHandler.state
        assertFalse(adaptiveKeepAliveStateHandler.isCurrentKeepAliveFailureLimitExceeded())
        val newState = adaptiveKeepAliveStateHandler.state

        verifyStates(oldState, newState)
    }

    @Test
    fun `test isCurrentKeepAliveFailureLimitExceeded should return true when limit is exceeded`() {
        adaptiveKeepAliveStateHandler.state = adaptiveKeepAliveStateHandler.state.copy(
            currentKAFailureCount = MAX_CURRENT_KEEPALIVE_FAILURE
        )
        val oldState = adaptiveKeepAliveStateHandler.state
        assertTrue(adaptiveKeepAliveStateHandler.isCurrentKeepAliveFailureLimitExceeded())
        val newState = adaptiveKeepAliveStateHandler.state

        verifyStates(oldState, newState)
    }

    @Test
    fun `test isOptimalKeepAliveFailureLimitExceeded should return false when limit is not exceeded`() {
        adaptiveKeepAliveStateHandler.state = adaptiveKeepAliveStateHandler.state.copy(
            optimalKAFailureCount = adaptiveKeepAliveStateHandler.state.optimalKeepAliveResetLimit - 1
        )
        val oldState = adaptiveKeepAliveStateHandler.state
        assertFalse(adaptiveKeepAliveStateHandler.isOptimalKeepAliveFailureLimitExceeded())
        val newState = adaptiveKeepAliveStateHandler.state

        verifyStates(oldState, newState)
    }

    @Test
    fun `test isOptimalKeepAliveFailureLimitExceeded should return true when limit is exceeded`() {
        adaptiveKeepAliveStateHandler.state = adaptiveKeepAliveStateHandler.state.copy(
            optimalKAFailureCount = adaptiveKeepAliveStateHandler.state.optimalKeepAliveResetLimit
        )
        val oldState = adaptiveKeepAliveStateHandler.state
        assertTrue(adaptiveKeepAliveStateHandler.isOptimalKeepAliveFailureLimitExceeded())
        val newState = adaptiveKeepAliveStateHandler.state

        verifyStates(oldState, newState)
    }

    @Test
    fun `test isOptimalKeepAliveFound`() {
        val oldState = adaptiveKeepAliveStateHandler.state
        assertEquals(oldState.isOptimalKeepAlive, adaptiveKeepAliveStateHandler.isOptimalKeepAliveFound())
        val newState = adaptiveKeepAliveStateHandler.state

        verifyStates(oldState, newState)
    }

    @Test
    fun `test getProbeCount`() {
        val oldState = adaptiveKeepAliveStateHandler.state
        assertEquals(oldState.probeCount, adaptiveKeepAliveStateHandler.getProbeCount())
        val newState = adaptiveKeepAliveStateHandler.state

        verifyStates(oldState, newState)
    }

    @Test
    fun `test getConvergenceTime`() {
        val oldState = adaptiveKeepAliveStateHandler.state
        assertEquals(oldState.convergenceTime, adaptiveKeepAliveStateHandler.getConvergenceTime())
        val newState = adaptiveKeepAliveStateHandler.state

        verifyStates(oldState, newState)
    }

    @Test
    fun `test isValidKeepAlive return false when network name is not same`() {
        val keepAlive = KeepAlive(
            networkName = "test-network",
            networkType = adaptiveKeepAliveStateHandler.state.currentNetworkType,
            keepAliveMinutes = adaptiveKeepAliveStateHandler.state.currentKA,
        )
        val oldState = adaptiveKeepAliveStateHandler.state
        assertFalse(adaptiveKeepAliveStateHandler.isValidKeepAlive(keepAlive))
        val newState = adaptiveKeepAliveStateHandler.state

        verifyStates(oldState, newState)
    }

    @Test
    fun `test isValidKeepAlive return false when network type is not same`() {
        val keepAlive = KeepAlive(
            networkName = adaptiveKeepAliveStateHandler.state.currentNetworkName,
            networkType = adaptiveKeepAliveStateHandler.state.currentNetworkType + 1,
            keepAliveMinutes = adaptiveKeepAliveStateHandler.state.currentKA,
        )
        val oldState = adaptiveKeepAliveStateHandler.state
        assertFalse(adaptiveKeepAliveStateHandler.isValidKeepAlive(keepAlive))
        val newState = adaptiveKeepAliveStateHandler.state

        verifyStates(oldState, newState)
    }

    @Test
    fun `test isValidKeepAlive return false when keep alive value is not same`() {
        val keepAlive = KeepAlive(
            networkName = adaptiveKeepAliveStateHandler.state.currentNetworkName,
            networkType = adaptiveKeepAliveStateHandler.state.currentNetworkType,
            keepAliveMinutes = adaptiveKeepAliveStateHandler.state.currentKA - 2,
        )
        val oldState = adaptiveKeepAliveStateHandler.state
        assertFalse(adaptiveKeepAliveStateHandler.isValidKeepAlive(keepAlive))
        val newState = adaptiveKeepAliveStateHandler.state

        verifyStates(oldState, newState)
    }

    @Test
    fun `test isValidKeepAlive return true when all values are same`() {
        val keepAlive = KeepAlive(
            networkName = adaptiveKeepAliveStateHandler.state.currentNetworkName,
            networkType = adaptiveKeepAliveStateHandler.state.currentNetworkType,
            keepAliveMinutes = adaptiveKeepAliveStateHandler.state.currentKA,
        )
        val oldState = adaptiveKeepAliveStateHandler.state
        assertTrue(adaptiveKeepAliveStateHandler.isValidKeepAlive(keepAlive))
        val newState = adaptiveKeepAliveStateHandler.state

        verifyStates(oldState, newState)
    }

    @After
    fun teardown() {
        verifyNoMoreInteractions(keepAlivePersistence)
    }

    private fun verifyStates(oldState: AdaptiveKeepAliveState, newState: AdaptiveKeepAliveState) {
        assertEquals(oldState.lastSuccessfulKA, newState.lastSuccessfulKA)
        assertEquals(oldState.isOptimalKeepAlive, newState.isOptimalKeepAlive)
        assertEquals(oldState.currentUpperBound, newState.currentUpperBound)
        assertEquals(oldState.currentStep, newState.currentStep)
        assertEquals(oldState.currentKA, newState.currentKA)
        assertEquals(oldState.currentKAFailureCount, newState.currentKAFailureCount)
        assertEquals(oldState.probeCount, newState.probeCount)
        assertEquals(oldState.convergenceTime, newState.convergenceTime)
        assertEquals(oldState.optimalKAFailureCount, newState.optimalKAFailureCount)
        assertEquals(oldState.currentNetworkType, newState.currentNetworkType)
        assertEquals(oldState.currentNetworkName, newState.currentNetworkName)
        assertEquals(oldState.lowerBound, newState.lowerBound)
        assertEquals(oldState.upperBound, newState.upperBound)
        assertEquals(oldState.step, newState.step)
        assertEquals(oldState.optimalKeepAliveResetLimit, newState.optimalKeepAliveResetLimit)
    }

    private fun setOldNetworkInfo() {
        with(adaptiveKeepAliveStateHandler) {
            state = state.copy(
                currentNetworkType = OLD_NETWORK_TYPE,
                currentNetworkName = OLD_NETWORK,
            )
        }
    }

    private fun setCurrentNetworkInfo() {
        with(adaptiveKeepAliveStateHandler) {
            state = state.copy(
                currentNetworkType = CURRENT_NETWORK_TYPE,
                currentNetworkName = CURRENT_NETWORK,
            )
        }
    }

    private fun getCurrentNetworkKey(): String {
        return "$CURRENT_NETWORK_TYPE:$CURRENT_NETWORK"
    }

    private fun getOldNetworkKey(): String {
        return "$OLD_NETWORK_TYPE:$OLD_NETWORK"
    }

    private fun verifyCurrentNetworkInfo() {
        assertEquals(adaptiveKeepAliveStateHandler.state.currentNetworkName , CURRENT_NETWORK)
        assertEquals(adaptiveKeepAliveStateHandler.state.currentNetworkType , CURRENT_NETWORK_TYPE)
    }

    private fun verifyOldNetworkInfo() {
        assertEquals(adaptiveKeepAliveStateHandler.state.currentNetworkName , OLD_NETWORK)
        assertEquals(adaptiveKeepAliveStateHandler.state.currentNetworkType , OLD_NETWORK_TYPE)
    }

    private fun getKeepAlivePersistenceModel(lowerBound: Int, upperBound: Int): KeepAlivePersistenceModel {
        return KeepAlivePersistenceModel(
            lastSuccessfulKeepAlive = 4,
            networkType = CURRENT_NETWORK_TYPE,
            networkName = CURRENT_NETWORK,
            lowerBound = lowerBound,
            upperBound = upperBound,
            isOptimalKeepAlive = true,
            step = 2,
            underTrialKeepAlive = 4,
            keepAliveFailureCount = 0,
            probeCount = 3,
            convergenceTime = 10,
            currentUpperBound = 5
        )
    }

    private fun getKeepAlivePersistenceModel(state: AdaptiveKeepAliveState): KeepAlivePersistenceModel {
        return KeepAlivePersistenceModel(
            lastSuccessfulKeepAlive = state.lastSuccessfulKA,
            networkType = state.currentNetworkType,
            networkName = state.currentNetworkName,
            lowerBound = state.lowerBound,
            upperBound = state.upperBound,
            isOptimalKeepAlive = state.isOptimalKeepAlive,
            step = state.currentStep,
            underTrialKeepAlive = state.currentKA,
            keepAliveFailureCount = state.currentKAFailureCount,
            probeCount = state.probeCount,
            convergenceTime = state.convergenceTime,
            currentUpperBound = state.currentUpperBound
        )
    }
}

private const val CURRENT_NETWORK = "current_network"
private const val CURRENT_NETWORK_TYPE = 1
private const val OLD_NETWORK = "old-network"
private const val OLD_NETWORK_TYPE = 0
