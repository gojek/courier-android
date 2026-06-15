package com.gojek.workmanager.pingsender

import com.gojek.courier.logging.ILogger
import com.gojek.courier.utils.Clock
import com.gojek.mqtt.pingsender.IPingSenderEvents
import com.gojek.mqtt.pingsender.KeepAlive
import com.gojek.mqtt.pingsender.KeepAliveCalculator
import com.gojek.mqtt.pingsender.PingActionCallback
import com.gojek.mqtt.pingsender.PingSenderComms
import com.gojek.mqtt.pingsender.keepAliveMillis
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import java.util.concurrent.TimeUnit
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class WorkManagerPingSenderAdaptiveTest {
    private val pingWorkScheduler = mock<PingWorkScheduler>()
    private val pingSenderConfig = mock<WorkManagerPingSenderConfig>()
    private val clock = mock<Clock>()
    private val comms = mock<PingSenderComms>()
    private val logger = mock<ILogger>()
    private val pingSenderEvents = mock<IPingSenderEvents>()
    private val keepAliveCalculator = mock<KeepAliveCalculator>()

    private val pingSender = WorkManagerPingSenderAdaptive(pingWorkScheduler, pingSenderConfig, clock)

    @Before
    fun setup() {
        pingSender.setPingEventHandler(pingSenderEvents)
        pingSender.setKeepAliveCalculator(keepAliveCalculator)
        pingSender.init(comms, logger)
    }

    @Test
    fun `test start`() {
        val keepaliveMinutes = 1
        val timeoutSeconds = 10L
        val keepAlive = mock<KeepAlive>()
        whenever(comms.keepAliveMillis).thenReturn(20000L)
        whenever(pingSenderConfig.timeoutSeconds).thenReturn(timeoutSeconds)
        whenever(keepAlive.keepAliveMinutes).thenReturn(keepaliveMinutes)
        whenever(keepAliveCalculator.getUnderTrialKeepAlive()).thenReturn(keepAlive)

        pingSender.start()

        verify(pingWorkScheduler).schedulePingWork(keepAlive.keepAliveMillis(), timeoutSeconds)
        verify(pingSenderEvents).mqttPingScheduled(keepAlive.keepAliveMillis() / 1000, keepAlive.keepAliveMillis() / 1000)
    }

    @Test
    fun `test stop`() {
        pingSender.stop()

        verify(pingWorkScheduler).cancelWork()
    }

    @Test
    fun `test sendPing when ping cannot be sent(token = null)`() {
        val testUri = "test-uri"
        val keepaliveMinutes = 1
        val keepAlive = mock<KeepAlive>()
        whenever(comms.serverURI).thenReturn(testUri)
        whenever(keepAlive.keepAliveMinutes).thenReturn(keepaliveMinutes)
        whenever(comms.sendPingRequest(any())).thenReturn(false)
        pingSender.adaptiveKeepAlive = keepAlive

        pingSender.sendPing {
            // do nothing
        }

        verify(pingSenderEvents).mqttPingInitiated(testUri, keepAlive.keepAliveMillis() / 1000)
        verify(pingSenderEvents).pingMqttTokenNull(testUri, keepAlive.keepAliveMillis() / 1000)
    }

    @Test
    fun `test sendPing when ping can be sent successfully`() {
        val testUri = "test-uri"
        val keepaliveMinutes = 1
        val startTime = TimeUnit.MILLISECONDS.toNanos(100)
        val endTime = TimeUnit.MILLISECONDS.toNanos(110)
        val keepAlive = mock<KeepAlive>()
        val timeoutSeconds = 10L
        whenever(comms.serverURI).thenReturn(testUri)
        whenever(keepAlive.keepAliveMinutes).thenReturn(keepaliveMinutes)
        whenever(comms.sendPingRequest(any())).thenReturn(true)
        whenever(clock.nanoTime()).thenReturn(startTime, endTime)
        whenever(keepAliveCalculator.getUnderTrialKeepAlive()).thenReturn(keepAlive)
        whenever(pingSenderConfig.timeoutSeconds).thenReturn(timeoutSeconds)
        pingSender.adaptiveKeepAlive = keepAlive

        var success: Boolean? = null
        pingSender.sendPing {
            success = it
        }

        verify(pingSenderEvents).mqttPingInitiated(testUri, keepAlive.keepAliveMillis() / 1000)

        val argumentCaptor = argumentCaptor<PingActionCallback>()
        verify(comms).sendPingRequest(argumentCaptor.capture())
        argumentCaptor.lastValue.onSuccess()
        assertTrue(success!!)
        verify(pingSenderEvents).pingEventSuccess(testUri, 10, keepAlive.keepAliveMillis() / 1000)
        verify(keepAliveCalculator).onKeepAliveSuccess(keepAlive)

        verify(pingWorkScheduler).schedulePingWork(keepAlive.keepAliveMillis(), timeoutSeconds)
        verify(pingSenderEvents).mqttPingScheduled(keepAlive.keepAliveMillis() / 1000, keepAlive.keepAliveMillis() / 1000)
    }

    @Test
    fun `test sendPing when ping cannot be sent successfully`() {
        val testUri = "test-uri"
        val keepaliveMinutes = 1
        val keepAlive = mock<KeepAlive>()
        val startTime = TimeUnit.MILLISECONDS.toNanos(100)
        val endTime = TimeUnit.MILLISECONDS.toNanos(110)
        whenever(comms.serverURI).thenReturn(testUri)
        whenever(keepAlive.keepAliveMinutes).thenReturn(keepaliveMinutes)
        whenever(comms.sendPingRequest(any())).thenReturn(true)
        whenever(clock.nanoTime()).thenReturn(startTime, endTime)
        pingSender.adaptiveKeepAlive = keepAlive

        var success: Boolean? = null
        pingSender.sendPing {
            success = it
        }

        verify(pingSenderEvents).mqttPingInitiated(testUri, keepAlive.keepAliveMillis() / 1000)

        val argumentCaptor = argumentCaptor<PingActionCallback>()
        verify(comms).sendPingRequest(argumentCaptor.capture())
        val exception = Exception("test")
        argumentCaptor.lastValue.onFailure(exception)
        assertFalse(success!!)
        verify(pingSenderEvents).pingEventFailure(testUri, 10, exception, keepAlive.keepAliveMillis() / 1000)
    }
}
