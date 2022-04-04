package com.gojek.keepalive

import com.gojek.keepalive.model.KeepAlivePersistenceModel
import com.gojek.keepalive.model.toKeepAlive
import com.gojek.keepalive.persistence.KeepAlivePersistence
import com.gojek.keepalive.utils.NetworkUtils
import com.gojek.mqtt.pingsender.KeepAlive
import com.gojek.networktracker.NetworkStateTracker
import com.google.gson.Gson
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@RunWith(MockitoJUnitRunner::class)
class OptimalKeepAliveCalculatorTest {
    private val networkTracker = mock<NetworkStateTracker>()
    private val networkUtils = mock<NetworkUtils>()
    private val keepAlivePersistence = mock<KeepAlivePersistence>()
    private val optimalKeepAliveObserver = mock<OptimalKeepAliveObserver>()
    private val gson = mock<Gson>()
    private val lowerBound: Int = 2
    private val upperBound: Int = 10
    private val step: Int = 2
    private val optimalKeepAliveResetLimit: Int = 30

    private val optimalKeepAliveCalculator = OptimalKeepAliveCalculator(
        networkTracker = networkTracker,
        networkUtils = networkUtils,
        persistence = keepAlivePersistence,
        optimalKeepAliveObserver = optimalKeepAliveObserver,
        lowerBound = lowerBound,
        upperBound = upperBound,
        step = step,
        optimalKeepAliveResetLimit = optimalKeepAliveResetLimit,
        gson = gson
    )

    @Test
    fun `test init with network info same as current network info`() {
        setCurrentNetworkInfo()

        val oldKeepAlive = optimalKeepAliveCalculator.getCurrentKeepAlive()
        optimalKeepAliveCalculator.initialise(CURRENT_NETWORK_TYPE, CURRENT_NETWORK)
        val newKeepAlive = optimalKeepAliveCalculator.getCurrentKeepAlive()

        assertEquals(oldKeepAlive, newKeepAlive)
    }

    @Test
    fun `test init with network info different from current network info and no info available in persistence`() {
        setOldNetworkInfo()
        whenever(keepAlivePersistence.has(getCurrentNetworKey())).thenReturn(false)

        val oldKeepAlive = optimalKeepAliveCalculator.getCurrentKeepAlive()
        optimalKeepAliveCalculator.initialise(CURRENT_NETWORK_TYPE, CURRENT_NETWORK)
        val newKeepAlive = optimalKeepAliveCalculator.getCurrentKeepAlive()

        assertNotEquals(oldKeepAlive, newKeepAlive)
        verifyCurrentNetworkInfo()
        verify(keepAlivePersistence).has(getCurrentNetworKey())
    }

    @Test
    fun `test init with network info different from current network info and keepalive info available in persistence`() {
        setOldNetworkInfo()
        whenever(keepAlivePersistence.has(getCurrentNetworKey())).thenReturn(true)
        whenever(keepAlivePersistence.get(getCurrentNetworKey(), ""))
            .thenReturn("test-network-info")
        whenever(gson.fromJson("test-network-info", KeepAlivePersistenceModel::class.java))
            .thenReturn(getKeepAlivePersistenceModel(lowerBound, upperBound))

        val oldKeepAlive = optimalKeepAliveCalculator.getCurrentKeepAlive()
        optimalKeepAliveCalculator.initialise(CURRENT_NETWORK_TYPE, CURRENT_NETWORK)
        val newKeepAlive = optimalKeepAliveCalculator.getCurrentKeepAlive()

        assertNotEquals(oldKeepAlive, newKeepAlive)
        assertTrue(optimalKeepAliveCalculator.isOptimalKeepAlive)
        verifyCurrentNetworkInfo()
        verify(keepAlivePersistence).has(getCurrentNetworKey())
        verify(keepAlivePersistence).get(getCurrentNetworKey(), "")
        verify(gson).fromJson("test-network-info", KeepAlivePersistenceModel::class.java)
    }

    @Test
    fun `test init with network info different from current network info and keepalive info available in persistence with different upper bound`() {
        setOldNetworkInfo()
        whenever(keepAlivePersistence.has(getCurrentNetworKey())).thenReturn(true)
        whenever(keepAlivePersistence.get(getCurrentNetworKey(), "")).thenReturn("test-network-info")
        whenever(gson.fromJson("test-network-info", KeepAlivePersistenceModel::class.java)).thenReturn(getKeepAlivePersistenceModel(lowerBound, upperBound+1))

        val oldKeepAlive = optimalKeepAliveCalculator.getCurrentKeepAlive()
        optimalKeepAliveCalculator.initialise(CURRENT_NETWORK_TYPE, CURRENT_NETWORK)
        val newKeepAlive = optimalKeepAliveCalculator.getCurrentKeepAlive()

        assertNotEquals(oldKeepAlive, newKeepAlive)
        assertFalse(optimalKeepAliveCalculator.isOptimalKeepAlive)
        verifyCurrentNetworkInfo()
        verify(keepAlivePersistence).has(getCurrentNetworKey())
        verify(keepAlivePersistence).get(getCurrentNetworKey(), "")
        verify(gson).fromJson("test-network-info", KeepAlivePersistenceModel::class.java)
    }

    @Test
    fun `test init with network info different from current network info and keepalive info available in persistence with different lower bound`() {
        setOldNetworkInfo()
        whenever(keepAlivePersistence.has(getCurrentNetworKey())).thenReturn(true)
        whenever(keepAlivePersistence.get(getCurrentNetworKey(), "")).thenReturn("test-network-info")
        whenever(gson.fromJson("test-network-info", KeepAlivePersistenceModel::class.java)).thenReturn(getKeepAlivePersistenceModel(lowerBound+1, upperBound))

        val oldKeepAlive = optimalKeepAliveCalculator.getCurrentKeepAlive()
        optimalKeepAliveCalculator.initialise(CURRENT_NETWORK_TYPE, CURRENT_NETWORK)
        val newKeepAlive = optimalKeepAliveCalculator.getCurrentKeepAlive()

        assertNotEquals(oldKeepAlive, newKeepAlive)
        assertFalse(optimalKeepAliveCalculator.isOptimalKeepAlive)
        verifyCurrentNetworkInfo()
        verify(keepAlivePersistence).has(getCurrentNetworKey())
        verify(keepAlivePersistence).get(getCurrentNetworKey(), "")
        verify(gson).fromJson("test-network-info", KeepAlivePersistenceModel::class.java)
    }

    @Test
    fun `test getKeepAlive with optimal keepalive already found`() {
        optimalKeepAliveCalculator.isOptimalKeepAlive = true
        setCurrentNetworkInfo()
        val keepAlivePersistenceModel = optimalKeepAliveCalculator.getKeepAlivePersistenceModel()
        val keepAlive = keepAlivePersistenceModel.toKeepAlive()
        whenever(gson.toJson(keepAlivePersistenceModel)).thenReturn("test-network-info")

        val optimalKeepAlive = optimalKeepAliveCalculator.getKeepAlive()

        assertEquals(keepAlive, optimalKeepAlive)
        verify(keepAlivePersistence).put(getCurrentNetworKey(), "test-network-info")
    }

    @Test
    fun `test calculateKeepAlive when optimal keepalive is found`()  {
        setCurrentNetworkInfo()
        optimalKeepAliveCalculator.lastSuccessfulKA = upperBound
        optimalKeepAliveCalculator.probeCount = 4
        optimalKeepAliveCalculator.convergenceTime = 10
        val persistenceModel = optimalKeepAliveCalculator.getKeepAlivePersistenceModel().copy(
            isOptimalKeepAlive = true,
            keepAliveFailureCount = 0
        )
        whenever(gson.toJson(persistenceModel)).thenReturn("test-network-info")

        optimalKeepAliveCalculator.calculateKeepAlive()

        verify(keepAlivePersistence).put(getCurrentNetworKey(), "test-network-info")
        verify(optimalKeepAliveObserver).onOptimalKeepAliveFound(upperBound, 4, 10)
    }

    @Test
    fun `test calculateKeepAlive when optimal keepalive is still to be found`()  {
        setCurrentNetworkInfo()
        optimalKeepAliveCalculator.lastSuccessfulKA = 4
        optimalKeepAliveCalculator.probeCount = 2
        optimalKeepAliveCalculator.convergenceTime = 6
        optimalKeepAliveCalculator.currentKAFailureCount = 0
        optimalKeepAliveCalculator.currentKA = 4


        optimalKeepAliveCalculator.calculateKeepAlive()

        assertEquals(0, optimalKeepAliveCalculator.currentKAFailureCount)
        assertEquals(6, optimalKeepAliveCalculator.currentKA)
        assertEquals(3, optimalKeepAliveCalculator.probeCount)
        assertEquals(12, optimalKeepAliveCalculator.convergenceTime)
    }

    @Test
    fun `test calculateKeepAlive when optimal keepalive is still to be found and currentKA is already tried`()  {
        setCurrentNetworkInfo()
        optimalKeepAliveCalculator.lastSuccessfulKA = 4
        optimalKeepAliveCalculator.probeCount = 2
        optimalKeepAliveCalculator.convergenceTime = 6
        optimalKeepAliveCalculator.currentKAFailureCount = 0
        optimalKeepAliveCalculator.currentKA = 6


        optimalKeepAliveCalculator.calculateKeepAlive()

        assertEquals(1, optimalKeepAliveCalculator.currentKAFailureCount)
        assertEquals(6, optimalKeepAliveCalculator.currentKA)
        assertEquals(3, optimalKeepAliveCalculator.probeCount)
        assertEquals(12, optimalKeepAliveCalculator.convergenceTime)
    }

    @Test
    fun `test getNetworkKey`() {
        setCurrentNetworkInfo()
        var networkKey = optimalKeepAliveCalculator.getNetworkKey()
        assertEquals(getCurrentNetworKey(), networkKey)

        setOldNetworkInfo()
        networkKey = optimalKeepAliveCalculator.getNetworkKey()
        assertEquals(getOldNetworKey(), networkKey)
    }

    @Test
    fun `test onKeepAliveSuccess with old network info`() {
        setOldNetworkInfo()
        val keepAlive = KeepAlive(CURRENT_NETWORK_TYPE, CURRENT_NETWORK, 5)

        optimalKeepAliveCalculator.onKeepAliveSuccess(keepAlive)

        verifyZeroInteractions(keepAlivePersistence)
    }

    @Test
    fun `test onKeepAliveSuccess with current network info`() {
        setCurrentNetworkInfo()
        optimalKeepAliveCalculator.lastSuccessfulKA = -1
        val keepAlive = KeepAlive(CURRENT_NETWORK_TYPE, CURRENT_NETWORK, 5)
        val persistenceModel = optimalKeepAliveCalculator.getKeepAlivePersistenceModel().copy(
            lastSuccessfulKeepAlive = 5,
            keepAliveFailureCount = 0
        )
        whenever(gson.toJson(persistenceModel)).thenReturn("test-network-info")

        optimalKeepAliveCalculator.onKeepAliveSuccess(keepAlive)

        assertEquals(keepAlive.underTrialKeepAlive, optimalKeepAliveCalculator.lastSuccessfulKA)
        assertEquals(0, optimalKeepAliveCalculator.currentKAFailureCount)
        verify(keepAlivePersistence).put(getCurrentNetworKey(), "test-network-info")
    }

    @Test
    fun `test onKeepAliveFailure with old network info`() {
        setOldNetworkInfo()
        val keepAlive = KeepAlive(CURRENT_NETWORK_TYPE, CURRENT_NETWORK, 5)

        optimalKeepAliveCalculator.onKeepAliveFailure(keepAlive)

        verifyZeroInteractions(keepAlivePersistence)
    }

    @Test
    fun `test onKeepAliveFailure when optimal keepalive is found and optimalKAFailureCount is less than optimalKeepAliveResetLimit`() {
        setCurrentNetworkInfo()
        optimalKeepAliveCalculator.isOptimalKeepAlive = true
        optimalKeepAliveCalculator.optimalKAFailureCount = optimalKeepAliveResetLimit - 2
        val keepAlive = KeepAlive(CURRENT_NETWORK_TYPE, CURRENT_NETWORK, 5)

        optimalKeepAliveCalculator.onKeepAliveFailure(keepAlive)

        verifyZeroInteractions(keepAlivePersistence)
    }

    @Test
    fun `test onKeepAliveFailure when optimal keepalive is found and optimalKAFailureCount is equal to optimalKeepAliveResetLimit`() {
        setCurrentNetworkInfo()
        optimalKeepAliveCalculator.isOptimalKeepAlive = true
        optimalKeepAliveCalculator.optimalKAFailureCount = optimalKeepAliveResetLimit - 1
        val keepAlive = KeepAlive(CURRENT_NETWORK_TYPE, CURRENT_NETWORK, 5)

        optimalKeepAliveCalculator.onKeepAliveFailure(keepAlive)

        verify(keepAlivePersistence).remove(getCurrentNetworKey())
    }

    @Test
    fun `test onKeepAliveFailure when optimal keepalive is found and optimalKAFailureCount is greater than optimalKeepAliveResetLimit`() {
        setCurrentNetworkInfo()
        optimalKeepAliveCalculator.isOptimalKeepAlive = true
        optimalKeepAliveCalculator.optimalKAFailureCount = optimalKeepAliveResetLimit
        val keepAlive = KeepAlive(CURRENT_NETWORK_TYPE, CURRENT_NETWORK, 5)

        optimalKeepAliveCalculator.onKeepAliveFailure(keepAlive)

        verify(keepAlivePersistence).remove(getCurrentNetworKey())
    }

    @Test
    fun `test onKeepAliveFailure when optimal keepalive is not found and underTrialKeepAlive is equal to lowerBound`() {
        setCurrentNetworkInfo()
        val keepAlive = KeepAlive(CURRENT_NETWORK_TYPE, CURRENT_NETWORK, lowerBound)
        optimalKeepAliveCalculator.probeCount = 4
        optimalKeepAliveCalculator.convergenceTime = 10
        val persistenceModel = optimalKeepAliveCalculator.getKeepAlivePersistenceModel().copy(
            lastSuccessfulKeepAlive = lowerBound,
            keepAliveFailureCount = 0,
            isOptimalKeepAlive = true
        )
        whenever(gson.toJson(persistenceModel)).thenReturn("test-network-info")

        optimalKeepAliveCalculator.onKeepAliveFailure(keepAlive)

        assertTrue(optimalKeepAliveCalculator.isOptimalKeepAlive)
        assertEquals(keepAlive.underTrialKeepAlive, optimalKeepAliveCalculator.lastSuccessfulKA)
        assertEquals(0, optimalKeepAliveCalculator.currentKAFailureCount)
        verify(keepAlivePersistence).put(getCurrentNetworKey(), "test-network-info")
        verify(optimalKeepAliveObserver).onOptimalKeepAliveNotFound(lowerBound, 4, 10)
    }

    @Test
    fun `test onKeepAliveFailure when optimal keepalive is not found and underTrialKeepAlive is not equal to lowerBound`() {
        setCurrentNetworkInfo()
        val keepAlive = KeepAlive(CURRENT_NETWORK_TYPE, CURRENT_NETWORK, lowerBound+1)

        optimalKeepAliveCalculator.onKeepAliveFailure(keepAlive)

        assertEquals(keepAlive.underTrialKeepAlive-1, optimalKeepAliveCalculator.currentUpperBound)
        assertEquals(step/2, optimalKeepAliveCalculator.currentStep)
    }

    @Test
    fun `test getOptimalKeepAlive when optimal keepalive is not found`() {
        optimalKeepAliveCalculator.isOptimalKeepAlive = false

        assertEquals(0, optimalKeepAliveCalculator.getOptimalKeepAlive())
    }

    @Test
    fun `test getOptimalKeepAlive when optimal keepalive is found`() {
        optimalKeepAliveCalculator.isOptimalKeepAlive = true
        optimalKeepAliveCalculator.lastSuccessfulKA = 5

        assertEquals(5, optimalKeepAliveCalculator.getOptimalKeepAlive())
    }

    @Test
    fun `test onOptimalKeepAliveFailure when optimal keepalive is not found`() {
        optimalKeepAliveCalculator.isOptimalKeepAlive = false

        optimalKeepAliveCalculator.onOptimalKeepAliveFailure()

        verifyZeroInteractions(keepAlivePersistence)
    }

    @Test
    fun `test onOptimalKeepAliveFailure when optimal keepalive is found and optimalKAFailureCount is less than optimalKeepAliveResetLimit`() {
        optimalKeepAliveCalculator.isOptimalKeepAlive = true
        optimalKeepAliveCalculator.optimalKAFailureCount = optimalKeepAliveResetLimit - 2

        optimalKeepAliveCalculator.onOptimalKeepAliveFailure()

        verifyZeroInteractions(keepAlivePersistence)
    }

    @Test
    fun `test onOptimalKeepAliveFailure when optimal keepalive is found and optimalKAFailureCount is equal to optimalKeepAliveResetLimit`() {
        setCurrentNetworkInfo()
        optimalKeepAliveCalculator.isOptimalKeepAlive = true
        optimalKeepAliveCalculator.optimalKAFailureCount = optimalKeepAliveResetLimit - 1

        optimalKeepAliveCalculator.onOptimalKeepAliveFailure()

        verify(keepAlivePersistence).remove(getCurrentNetworKey())
    }

    @Test
    fun `test onOptimalKeepAliveFailure when optimal keepalive is found and optimalKAFailureCount is greater than optimalKeepAliveResetLimit`() {
        setCurrentNetworkInfo()
        optimalKeepAliveCalculator.isOptimalKeepAlive = true
        optimalKeepAliveCalculator.optimalKAFailureCount = optimalKeepAliveResetLimit

        optimalKeepAliveCalculator.onOptimalKeepAliveFailure()

        verify(keepAlivePersistence).remove(getCurrentNetworKey())
    }

    @Test
    fun `test getCurrentKeepAlive`() {
        setCurrentNetworkInfo()
        optimalKeepAliveCalculator.currentKA = 10
        var currentKeepAlive = optimalKeepAliveCalculator.getCurrentKeepAlive()
        assertEquals(10, currentKeepAlive.underTrialKeepAlive)
        verifyCurrentNetworkInfo()

        setOldNetworkInfo()
        optimalKeepAliveCalculator.currentKA = 12
        currentKeepAlive = optimalKeepAliveCalculator.getCurrentKeepAlive()
        assertEquals(12, currentKeepAlive.underTrialKeepAlive)
        verifyOldNetworkInfo()
    }

    @After
    fun teardown() {
        verifyNoMoreInteractions(keepAlivePersistence, optimalKeepAliveObserver)
    }

    private fun setOldNetworkInfo() {
        optimalKeepAliveCalculator.currentNetworkName = OLD_NETWORK
        optimalKeepAliveCalculator.currentNetworkType = OLD_NETWORK_TYPE
    }

    private fun setCurrentNetworkInfo() {
        optimalKeepAliveCalculator.currentNetworkName = CURRENT_NETWORK
        optimalKeepAliveCalculator.currentNetworkType = CURRENT_NETWORK_TYPE
    }

    private fun getCurrentNetworKey(): String {
        return "$CURRENT_NETWORK_TYPE:$CURRENT_NETWORK"
    }

    private fun getOldNetworKey(): String {
        return "$OLD_NETWORK_TYPE:$OLD_NETWORK"
    }

    private fun verifyCurrentNetworkInfo() {
        assertEquals(optimalKeepAliveCalculator.currentNetworkName , CURRENT_NETWORK)
        assertEquals(optimalKeepAliveCalculator.currentNetworkType , CURRENT_NETWORK_TYPE)
    }

    private fun verifyOldNetworkInfo() {
        assertEquals(optimalKeepAliveCalculator.currentNetworkName , OLD_NETWORK)
        assertEquals(optimalKeepAliveCalculator.currentNetworkType , OLD_NETWORK_TYPE)
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
            convergenceTime = 10
        )
    }
}

private const val CURRENT_NETWORK = "current_network"
private const val CURRENT_NETWORK_TYPE = 1
private const val OLD_NETWORK = "old-network"
private const val OLD_NETWORK_TYPE = 0