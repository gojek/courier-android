package com.gojek.workmanager.pingsender

import com.gojek.courier.utils.Clock
import com.gojek.mqtt.pingsender.IPingSenderEvents
import com.gojek.mqtt.pingsender.KeepAlive
import com.gojek.mqtt.pingsender.KeepAliveCalculator
import com.gojek.mqtt.pingsender.keepAliveMillis
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.eclipse.paho.client.mqttv3.ILogger
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttAsyncClient
import org.eclipse.paho.client.mqttv3.MqttToken
import org.eclipse.paho.client.mqttv3.internal.ClientComms
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import java.util.concurrent.TimeUnit
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(MockitoJUnitRunner::class)
class WorkManagerPingSenderAdaptiveTest {
    private val pingWorkScheduler = mock<PingWorkScheduler>()
    private val pingSenderConfig = mock<WorkManagerPingSenderConfig>()
    private val clock = mock<Clock>()
    private val comms = mock<ClientComms>()
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
        whenever(comms.keepAlive).thenReturn(20000L)
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
        val mqttClient = mock<IMqttAsyncClient>()
        val testUri = "test-uri"
        val keepaliveMinutes = 1
        val keepAlive = mock<KeepAlive>()
        whenever(comms.client).thenReturn(mqttClient)
        whenever(mqttClient.serverURI).thenReturn(testUri)
        whenever(keepAlive.keepAliveMinutes).thenReturn(keepaliveMinutes)
        whenever(comms.sendPingRequest()).thenReturn(null)
        pingSender.adaptiveKeepAlive = keepAlive

        pingSender.sendPing {
            // do nothing
        }

        verify(pingSenderEvents).mqttPingInitiated(testUri, keepAlive.keepAliveMillis() / 1000)
        verify(pingSenderEvents).pingMqttTokenNull(testUri, keepAlive.keepAliveMillis() / 1000)
    }

    @Test
    fun `test sendPing when ping can be sent successfully`() {
        val mqttClient = mock<IMqttAsyncClient>()
        val mqttToken = mock<MqttToken>()
        val testUri = "test-uri"
        val keepaliveMinutes = 1
        val startTime = TimeUnit.MILLISECONDS.toNanos(100)
        val endTime = TimeUnit.MILLISECONDS.toNanos(110)
        val keepAlive = mock<KeepAlive>()
        val timeoutSeconds = 10L
        whenever(comms.client).thenReturn(mqttClient)
        whenever(mqttClient.serverURI).thenReturn(testUri)
        whenever(keepAlive.keepAliveMinutes).thenReturn(keepaliveMinutes)
        whenever(comms.sendPingRequest()).thenReturn(mqttToken)
        whenever(clock.nanoTime()).thenReturn(startTime, endTime)
        whenever(mqttToken.userContext).thenReturn(keepAlive)
        whenever(keepAliveCalculator.getUnderTrialKeepAlive()).thenReturn(keepAlive)
        whenever(pingSenderConfig.timeoutSeconds).thenReturn(timeoutSeconds)
        pingSender.adaptiveKeepAlive = keepAlive

        var success: Boolean? = null
        pingSender.sendPing {
            success = it
        }

        verify(pingSenderEvents).mqttPingInitiated(testUri, keepAlive.keepAliveMillis() / 1000)

        val argumentCaptor = argumentCaptor<IMqttActionListener>()
        verify(mqttToken).actionCallback = argumentCaptor.capture()
        argumentCaptor.lastValue.onSuccess(mqttToken)
        assertTrue(success!!)
        verify(pingSenderEvents).pingEventSuccess(testUri, 10, keepAlive.keepAliveMillis() / 1000)
        verify(keepAliveCalculator).onKeepAliveSuccess(keepAlive)

        verify(pingWorkScheduler).schedulePingWork(keepAlive.keepAliveMillis(), timeoutSeconds)
        verify(pingSenderEvents).mqttPingScheduled(keepAlive.keepAliveMillis() / 1000, keepAlive.keepAliveMillis() / 1000)
    }

    @Test
    fun `test sendPing when ping cannot be sent successfully`() {
        val mqttClient = mock<IMqttAsyncClient>()
        val mqttToken = mock<MqttToken>()
        val testUri = "test-uri"
        val keepaliveMinutes = 1
        val keepAlive = mock<KeepAlive>()
        val startTime = TimeUnit.MILLISECONDS.toNanos(100)
        val endTime = TimeUnit.MILLISECONDS.toNanos(110)
        whenever(comms.client).thenReturn(mqttClient)
        whenever(mqttClient.serverURI).thenReturn(testUri)
        whenever(keepAlive.keepAliveMinutes).thenReturn(keepaliveMinutes)
        whenever(comms.sendPingRequest()).thenReturn(mqttToken)
        whenever(clock.nanoTime()).thenReturn(startTime, endTime)
        whenever(mqttToken.userContext).thenReturn(keepAlive)
        pingSender.adaptiveKeepAlive = keepAlive

        var success: Boolean? = null
        pingSender.sendPing {
            success = it
        }

        verify(pingSenderEvents).mqttPingInitiated(testUri, keepAlive.keepAliveMillis() / 1000)

        val argumentCaptor = argumentCaptor<IMqttActionListener>()
        verify(mqttToken).actionCallback = argumentCaptor.capture()
        val exception = Exception("test")
        argumentCaptor.lastValue.onFailure(mqttToken, exception)
        assertFalse(success!!)
        verify(pingSenderEvents).pingEventFailure(testUri, 10, exception, keepAlive.keepAliveMillis() / 1000)
    }
}
