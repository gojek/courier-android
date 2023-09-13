package com.gojek.timer.pingsender

import com.gojek.courier.utils.Clock
import com.gojek.mqtt.pingsender.IPingSenderEvents
import com.gojek.timer.pingsender.TimerPingSender.PingTask
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import java.util.Timer
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import org.eclipse.paho.client.mqttv3.ILogger
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttAsyncClient
import org.eclipse.paho.client.mqttv3.MqttToken
import org.eclipse.paho.client.mqttv3.internal.ClientComms
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class TimerPingSenderTest {
    private val clock = mock<Clock>()
    private val timerFactory = mock<TimerFactory>()
    private val comms = mock<ClientComms>()
    private val logger = mock<ILogger>()
    private val pingSenderEvents = mock<IPingSenderEvents>()
    private val pingSenderConfig = mock<TimerPingSenderConfig>()

    private val pingSender = TimerPingSender(pingSenderConfig, clock, timerFactory)

    @Before
    fun setup() {
        pingSender.setPingEventHandler(pingSenderEvents)
        pingSender.init(comms, logger)
    }

    @Test
    fun `test start`() {
        val mqttClient = mock<IMqttAsyncClient>()
        val timer = mock<Timer>()
        val clientId = "test-client"
        val keepAliveMillis = 10000L
        whenever(comms.client).thenReturn(mqttClient)
        whenever(comms.keepAlive).thenReturn(keepAliveMillis)
        whenever(mqttClient.clientId).thenReturn(clientId)
        whenever(timerFactory.getTimer(any())).thenReturn(timer)

        pingSender.start()

        val argumentCaptor1 = argumentCaptor<PingTask>()
        val argumentCaptor2 = argumentCaptor<Long>()
        verify(timer).schedule(argumentCaptor1.capture(), argumentCaptor2.capture())
        assertEquals(keepAliveMillis, argumentCaptor2.lastValue)
        verify(pingSenderEvents).mqttPingScheduled(keepAliveMillis / 1000, keepAliveMillis / 1000)
    }

    @Test
    fun `test stop`() {
        val timer = mock<Timer>()
        pingSender.timer = timer

        pingSender.stop()

        verify(timer).cancel()
    }

    @Test
    fun `test sendPing when ping cannot be sent(token = null)`() {
        val mqttClient = mock<IMqttAsyncClient>()
        val testUri = "test-uri"
        val keepaliveMillis = 30000L
        whenever(pingSenderConfig.sendForcePing).thenReturn(false)
        whenever(comms.client).thenReturn(mqttClient)
        whenever(mqttClient.serverURI).thenReturn(testUri)
        whenever(comms.keepAlive).thenReturn(keepaliveMillis)
        whenever(comms.checkForActivity(false)).thenReturn(null)

        pingSender.PingTask().run()

        verify(pingSenderEvents).mqttPingInitiated(testUri, keepaliveMillis / 1000)
        verify(pingSenderEvents).pingMqttTokenNull(testUri, keepaliveMillis / 1000)
    }

    @Test
    fun `test sendPing when ping can be sent successfully`() {
        val mqttClient = mock<IMqttAsyncClient>()
        val mqttToken = mock<MqttToken>()
        val testUri = "test-uri"
        val keepaliveMillis = 30000L
        val startTime = TimeUnit.MILLISECONDS.toNanos(100)
        val endTime = TimeUnit.MILLISECONDS.toNanos(110)
        whenever(pingSenderConfig.sendForcePing).thenReturn(false)
        whenever(comms.client).thenReturn(mqttClient)
        whenever(mqttClient.serverURI).thenReturn(testUri)
        whenever(comms.keepAlive).thenReturn(keepaliveMillis)
        whenever(comms.checkForActivity(false)).thenReturn(mqttToken)
        whenever(clock.nanoTime()).thenReturn(startTime, endTime)

        pingSender.PingTask().run()

        verify(pingSenderEvents).mqttPingInitiated(testUri, keepaliveMillis / 1000)

        val argumentCaptor = argumentCaptor<IMqttActionListener>()
        verify(mqttToken).actionCallback = argumentCaptor.capture()
        argumentCaptor.lastValue.onSuccess(mqttToken)
        verify(pingSenderEvents).pingEventSuccess(testUri, 10, keepaliveMillis / 1000)
    }

    @Test
    fun `test sendPing when ping can be sent successfully with sendForcePing=true`() {
        val mqttClient = mock<IMqttAsyncClient>()
        val mqttToken = mock<MqttToken>()
        val testUri = "test-uri"
        val keepaliveMillis = 30000L
        val startTime = TimeUnit.MILLISECONDS.toNanos(100)
        val endTime = TimeUnit.MILLISECONDS.toNanos(110)
        whenever(pingSenderConfig.sendForcePing).thenReturn(true)
        whenever(comms.client).thenReturn(mqttClient)
        whenever(mqttClient.serverURI).thenReturn(testUri)
        whenever(comms.keepAlive).thenReturn(keepaliveMillis)
        whenever(comms.checkForActivity(true)).thenReturn(mqttToken)
        whenever(clock.nanoTime()).thenReturn(startTime, endTime)

        pingSender.PingTask().run()

        verify(pingSenderEvents).mqttPingInitiated(testUri, keepaliveMillis / 1000)

        val argumentCaptor = argumentCaptor<IMqttActionListener>()
        verify(mqttToken).actionCallback = argumentCaptor.capture()
        argumentCaptor.lastValue.onSuccess(mqttToken)
        verify(pingSenderEvents).pingEventSuccess(testUri, 10, keepaliveMillis / 1000)
    }

    @Test
    fun `test sendPing when ping cannot be sent successfully`() {
        val mqttClient = mock<IMqttAsyncClient>()
        val mqttToken = mock<MqttToken>()
        val testUri = "test-uri"
        val keepaliveMillis = 30000L
        val startTime = TimeUnit.MILLISECONDS.toNanos(100)
        val endTime = TimeUnit.MILLISECONDS.toNanos(110)
        whenever(pingSenderConfig.sendForcePing).thenReturn(false)
        whenever(comms.client).thenReturn(mqttClient)
        whenever(mqttClient.serverURI).thenReturn(testUri)
        whenever(comms.keepAlive).thenReturn(keepaliveMillis)
        whenever(comms.checkForActivity(false)).thenReturn(mqttToken)
        whenever(clock.nanoTime()).thenReturn(startTime, endTime)

        pingSender.PingTask().run()

        verify(pingSenderEvents).mqttPingInitiated(testUri, keepaliveMillis / 1000)

        val argumentCaptor = argumentCaptor<IMqttActionListener>()
        verify(mqttToken).actionCallback = argumentCaptor.capture()
        val exception = Exception("test")
        argumentCaptor.lastValue.onFailure(mqttToken, exception)
        verify(pingSenderEvents).pingEventFailure(testUri, 10, exception, keepaliveMillis / 1000)
    }
}
