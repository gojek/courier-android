package com.gojek.workmanager.pingsender

import com.gojek.courier.logging.ILogger
import com.gojek.courier.utils.Clock
import com.gojek.mqtt.pingsender.IPingSenderEvents
import com.gojek.mqtt.pingsender.PingActionCallback
import com.gojek.mqtt.pingsender.PingSenderComms
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.eq
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
class WorkManagerPingSenderTest {
    private val pingWorkScheduler = mock<PingWorkScheduler>()
    private val pingSenderConfig = mock<WorkManagerPingSenderConfig>()
    private val clock = mock<Clock>()
    private val comms = mock<PingSenderComms>()
    private val logger = mock<ILogger>()
    private val pingSenderEvents = mock<IPingSenderEvents>()

    private val pingSender = WorkManagerPingSender(pingWorkScheduler, pingSenderConfig, clock)

    @Before
    fun setup() {
        pingSender.setPingEventHandler(pingSenderEvents)
        pingSender.init(comms, logger)
    }

    @Test
    fun `test start`() {
        val keepaliveMillis = 30000L
        val timeoutSeconds = 10000L
        whenever(comms.keepAliveMillis).thenReturn(keepaliveMillis)
        whenever(pingSenderConfig.timeoutSeconds).thenReturn(timeoutSeconds)

        pingSender.start()

        verify(pingWorkScheduler).schedulePingWork(keepaliveMillis, timeoutSeconds)
        verify(pingSenderEvents).mqttPingScheduled(keepaliveMillis / 1000, keepaliveMillis / 1000)
    }

    @Test
    fun `test stop`() {
        pingSender.stop()

        verify(pingWorkScheduler).cancelWork()
    }

    @Test
    fun `test sendPing when ping cannot be sent(token = null)`() {
        val testUri = "test-uri"
        val keepaliveMillis = 30000L
        whenever(pingSenderConfig.sendForcePing).thenReturn(false)
        whenever(comms.serverURI).thenReturn(testUri)
        whenever(comms.keepAliveMillis).thenReturn(keepaliveMillis)
        whenever(comms.checkActivity(eq(false), any())).thenReturn(false)

        pingSender.sendPing {
            // do nothing
        }

        verify(pingSenderEvents).mqttPingInitiated(testUri, keepaliveMillis / 1000)
        verify(pingSenderEvents).pingMqttTokenNull(testUri, keepaliveMillis / 1000)
    }

    @Test
    fun `test sendPing when ping can be sent successfully`() {
        val testUri = "test-uri"
        val keepaliveMillis = 30000L
        val startTime = TimeUnit.MILLISECONDS.toNanos(100)
        val endTime = TimeUnit.MILLISECONDS.toNanos(110)
        whenever(pingSenderConfig.sendForcePing).thenReturn(false)
        whenever(comms.serverURI).thenReturn(testUri)
        whenever(comms.keepAliveMillis).thenReturn(keepaliveMillis)
        whenever(comms.checkActivity(eq(false), any())).thenReturn(true)
        whenever(clock.nanoTime()).thenReturn(startTime, endTime)

        var success: Boolean? = null
        pingSender.sendPing {
            success = it
        }

        verify(pingSenderEvents).mqttPingInitiated(testUri, keepaliveMillis / 1000)

        val argumentCaptor = argumentCaptor<PingActionCallback>()
        verify(comms).checkActivity(eq(false), argumentCaptor.capture())
        argumentCaptor.lastValue.onSuccess()
        assertTrue(success!!)
        verify(pingSenderEvents).pingEventSuccess(testUri, 10, keepaliveMillis / 1000)
    }

    @Test
    fun `test sendPing when ping can be sent successfully with sendForcePing=true`() {
        val testUri = "test-uri"
        val keepaliveMillis = 30000L
        val startTime = TimeUnit.MILLISECONDS.toNanos(100)
        val endTime = TimeUnit.MILLISECONDS.toNanos(110)
        whenever(pingSenderConfig.sendForcePing).thenReturn(true)
        whenever(comms.serverURI).thenReturn(testUri)
        whenever(comms.keepAliveMillis).thenReturn(keepaliveMillis)
        whenever(comms.checkActivity(eq(true), any())).thenReturn(true)
        whenever(clock.nanoTime()).thenReturn(startTime, endTime)

        var success: Boolean? = null
        pingSender.sendPing {
            success = it
        }

        verify(pingSenderEvents).mqttPingInitiated(testUri, keepaliveMillis / 1000)

        val argumentCaptor = argumentCaptor<PingActionCallback>()
        verify(comms).checkActivity(eq(true), argumentCaptor.capture())
        argumentCaptor.lastValue.onSuccess()
        assertTrue(success!!)
        verify(pingSenderEvents).pingEventSuccess(testUri, 10, keepaliveMillis / 1000)
    }

    @Test
    fun `test sendPing when ping cannot be sent successfully`() {
        val testUri = "test-uri"
        val keepaliveMillis = 30000L
        val startTime = TimeUnit.MILLISECONDS.toNanos(100)
        val endTime = TimeUnit.MILLISECONDS.toNanos(110)
        whenever(pingSenderConfig.sendForcePing).thenReturn(false)
        whenever(comms.serverURI).thenReturn(testUri)
        whenever(comms.keepAliveMillis).thenReturn(keepaliveMillis)
        whenever(comms.checkActivity(eq(false), any())).thenReturn(true)
        whenever(clock.nanoTime()).thenReturn(startTime, endTime)

        var success: Boolean? = null
        pingSender.sendPing {
            success = it
        }

        verify(pingSenderEvents).mqttPingInitiated(testUri, keepaliveMillis / 1000)

        val argumentCaptor = argumentCaptor<PingActionCallback>()
        verify(comms).checkActivity(eq(false), argumentCaptor.capture())
        val exception = Exception("test")
        argumentCaptor.lastValue.onFailure(exception)
        assertFalse(success!!)
        verify(pingSenderEvents).pingEventFailure(testUri, 10, exception, keepaliveMillis / 1000)
    }
}
