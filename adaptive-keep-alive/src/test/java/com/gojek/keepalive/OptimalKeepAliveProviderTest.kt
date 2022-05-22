package com.gojek.keepalive

import android.content.Context
import com.gojek.keepalive.config.AdaptiveKeepAliveConfig
import com.gojek.mqtt.pingsender.KeepAliveCalculator
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import kotlin.test.assertEquals

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
    fun `test getOptimalKASecondsForCurrentNetwork should init keepAliveCalculator and get current keepalive`() {
        val optimalKeepAliveMinutes = 5
        whenever(keepAliveCalculator.getOptimalKeepAlive()).thenReturn(optimalKeepAliveMinutes)

        assertEquals(optimalKeepAliveMinutes * 60, optimalKeepAliveProvider.getOptimalKASecondsForCurrentNetwork())

        verify(keepAliveCalculator).getOptimalKeepAlive()
    }

    @Test
    fun `test onOptimalKeepAliveFailure should init keepAliveCalculator and invoke onOptimalKeepAliveFailure`() {
        optimalKeepAliveProvider.onOptimalKeepAliveFailure()

        verify(keepAliveCalculator).onOptimalKeepAliveFailure()
    }
}
