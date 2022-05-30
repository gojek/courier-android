package com.gojek.keepalive

import android.content.Context
import com.gojek.keepalive.config.AdaptiveKeepAliveConfig
import com.gojek.mqtt.pingsender.KeepAliveCalculator
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlin.test.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class OptimalKeepAliveProviderTest {
    private val context = mock<Context>()
    private val optimalKeepAliveObserver = mock<OptimalKeepAliveObserver>()
    private val keepAliveCalculatorFactory = mock<KeepAliveCalculatorFactory>()
    private val keepAliveCalculator = mock<KeepAliveCalculator>()
    private val adaptiveKeepAliveConfig = mock<AdaptiveKeepAliveConfig>()

    private lateinit var optimalKeepAliveProvider: OptimalKeepAliveProvider

    @Before
    fun setup() {
        whenever(
            keepAliveCalculatorFactory.create(
                context = context,
                adaptiveKeepAliveConfig = adaptiveKeepAliveConfig,
                optimalKeepAliveObserver = optimalKeepAliveObserver
            )
        ).thenReturn(keepAliveCalculator)

        optimalKeepAliveProvider = OptimalKeepAliveProvider(
            context = context,
            adaptiveKeepAliveConfig = adaptiveKeepAliveConfig,
            optimalKeepAliveObserver = optimalKeepAliveObserver,
            keepAliveCalculatorFactory = keepAliveCalculatorFactory
        )
    }

    @Test
    fun `test getOptimalKASecondsForCurrentNetwork should init KACalculator and get current KA`() {
        val optimalKeepAliveMinutes = 5
        whenever(keepAliveCalculator.getOptimalKeepAlive()).thenReturn(optimalKeepAliveMinutes)

        assertEquals(
            expected = optimalKeepAliveMinutes * 60,
            actual = optimalKeepAliveProvider.getOptimalKASecondsForCurrentNetwork()
        )

        verify(keepAliveCalculator).getOptimalKeepAlive()
    }

    @Test
    fun `test onOptimalKAFailure should init KACalculator & invoke onOptimalKAFailure`() {
        optimalKeepAliveProvider.onOptimalKeepAliveFailure()

        verify(keepAliveCalculator).onOptimalKeepAliveFailure()
    }
}
