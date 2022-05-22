package com.gojek.mqtt.exception.handler.v3.impl

import com.gojek.courier.logging.ILogger
import com.gojek.mqtt.constants.SERVER_UNAVAILABLE_MAX_CONNECT_TIME
import com.gojek.mqtt.policies.connectretrytime.IConnectRetryTimePolicy
import com.gojek.mqtt.scheduler.IRunnableScheduler
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttException.REASON_CODE_BROKER_UNAVAILABLE
import org.eclipse.paho.client.mqttv3.MqttException.REASON_CODE_CLIENT_ALREADY_DISCONNECTED
import org.eclipse.paho.client.mqttv3.MqttException.REASON_CODE_CLIENT_CLOSED
import org.eclipse.paho.client.mqttv3.MqttException.REASON_CODE_CLIENT_CONNECTED
import org.eclipse.paho.client.mqttv3.MqttException.REASON_CODE_CLIENT_DISCONNECTING
import org.eclipse.paho.client.mqttv3.MqttException.REASON_CODE_CLIENT_DISCONNECT_PROHIBITED
import org.eclipse.paho.client.mqttv3.MqttException.REASON_CODE_CLIENT_EXCEPTION
import org.eclipse.paho.client.mqttv3.MqttException.REASON_CODE_CLIENT_NOT_CONNECTED
import org.eclipse.paho.client.mqttv3.MqttException.REASON_CODE_CLIENT_TIMEOUT
import org.eclipse.paho.client.mqttv3.MqttException.REASON_CODE_CONNECTION_LOST
import org.eclipse.paho.client.mqttv3.MqttException.REASON_CODE_CONNECT_IN_PROGRESS
import org.eclipse.paho.client.mqttv3.MqttException.REASON_CODE_FAILED_AUTHENTICATION
import org.eclipse.paho.client.mqttv3.MqttException.REASON_CODE_INVALID_CLIENT_ID
import org.eclipse.paho.client.mqttv3.MqttException.REASON_CODE_INVALID_MESSAGE
import org.eclipse.paho.client.mqttv3.MqttException.REASON_CODE_INVALID_PROTOCOL_VERSION
import org.eclipse.paho.client.mqttv3.MqttException.REASON_CODE_MAX_INFLIGHT
import org.eclipse.paho.client.mqttv3.MqttException.REASON_CODE_NOT_AUTHORIZED
import org.eclipse.paho.client.mqttv3.MqttException.REASON_CODE_NO_MESSAGE_IDS_AVAILABLE
import org.eclipse.paho.client.mqttv3.MqttException.REASON_CODE_SERVER_CONNECT_ERROR
import org.eclipse.paho.client.mqttv3.MqttException.REASON_CODE_SOCKET_FACTORY_MISMATCH
import org.eclipse.paho.client.mqttv3.MqttException.REASON_CODE_SSL_CONFIG_ERROR
import org.eclipse.paho.client.mqttv3.MqttException.REASON_CODE_TOKEN_INUSE
import org.eclipse.paho.client.mqttv3.MqttException.REASON_CODE_UNEXPECTED_ERROR
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.nio.channels.UnresolvedAddressException
import java.util.Random
import javax.net.ssl.SSLHandshakeException

@RunWith(MockitoJUnitRunner::class)
class MqttExceptionHandlerImplTest {
    private val runnableScheduler = mock<IRunnableScheduler>()
    private val connectRetryTimePolicy = mock<IConnectRetryTimePolicy>()
    private val logger = mock<ILogger>()
    private val random = mock<Random>()
    private val mqttExceptionHandlerImpl = MqttExceptionHandlerImpl(
        runnableScheduler = runnableScheduler,
        connectRetryTimePolicy = connectRetryTimePolicy,
        logger = logger,
        random = random
    )

    @Test
    fun `test exception with reason code 0 with cause=null`() {
        val exception = MqttException(REASON_CODE_CLIENT_EXCEPTION.toInt(), null)
        whenever(connectRetryTimePolicy.getConnRetryTimeSecs()).thenReturn(20)
        mqttExceptionHandlerImpl.handleException(exception, false)
        verify(runnableScheduler).scheduleNextConnectionCheck(20)
    }

    @Test
    fun `test exception with reason code 0 with cause=UnknownHostException`() {
        val cause = UnknownHostException()
        val exception = MqttException(REASON_CODE_CLIENT_EXCEPTION.toInt(), cause)
        whenever(connectRetryTimePolicy.getConnRetryTimeSecs()).thenReturn(20)
        mqttExceptionHandlerImpl.handleException(exception, false)
        verify(runnableScheduler).scheduleNextConnectionCheck(20)
    }

    @Test
    fun `test exception with reason code 0 with cause=SocketException`() {
        val cause = SocketException()
        val exception = MqttException(REASON_CODE_CLIENT_EXCEPTION.toInt(), cause)
        whenever(connectRetryTimePolicy.getConnRetryTimeSecs()).thenReturn(15)
        mqttExceptionHandlerImpl.handleException(exception, false)
        verify(runnableScheduler).scheduleNextConnectionCheck(15)
    }

    @Test
    fun `test exception with reason code 0 with cause=SocketException and message as unresolved`() {
        val cause = SocketException("unresolved exception")
        val exception = MqttException(REASON_CODE_CLIENT_EXCEPTION.toInt(), cause)
        whenever(connectRetryTimePolicy.getConnRetryTimeSecs()).thenReturn(20)
        mqttExceptionHandlerImpl.handleException(exception, false)
        verify(runnableScheduler).scheduleNextConnectionCheck(20)
    }

    @Test
    fun `test exception with reason code 0 with cause=SocketTimeoutException`() {
        val cause = SocketTimeoutException()
        val exception = MqttException(REASON_CODE_CLIENT_EXCEPTION.toInt(), cause)
        whenever(connectRetryTimePolicy.getConnRetryTimeSecs()).thenReturn(20)
        mqttExceptionHandlerImpl.handleException(exception, false)
        verify(runnableScheduler).scheduleNextConnectionCheck(20)
    }

    @Test
    fun `test exception with reason code 0 with cause=UnresolvedAddressException`() {
        val cause = UnresolvedAddressException()
        val exception = MqttException(REASON_CODE_CLIENT_EXCEPTION.toInt(), cause)
        whenever(connectRetryTimePolicy.getConnRetryTimeSecs()).thenReturn(20)
        mqttExceptionHandlerImpl.handleException(exception, false)
        verify(runnableScheduler).scheduleNextConnectionCheck(20)
    }

    @Test
    fun `test exception with reason code 0 with cause=SSLHandshakeException`() {
        val cause = SSLHandshakeException("test")
        val exception = MqttException(REASON_CODE_CLIENT_EXCEPTION.toInt(), cause)
        whenever(connectRetryTimePolicy.getConnRetryTimeSecs()).thenReturn(5)
        mqttExceptionHandlerImpl.handleException(exception, false)
        verify(runnableScheduler).scheduleNextConnectionCheck(5)
    }

    @Test
    fun `test exception with reason code 0 with cause=Exception`() {
        val cause = Exception("test")
        val exception = MqttException(REASON_CODE_CLIENT_EXCEPTION.toInt(), cause)
        whenever(connectRetryTimePolicy.getConnRetryTimeSecs()).thenReturn(6)
        mqttExceptionHandlerImpl.handleException(exception, false)
        verify(runnableScheduler).scheduleNextConnectionCheck(6)
    }

    @Test
    fun `test exception with reason code 0 with cause=Exception and reconnect=true`() {
        val cause = Exception("test")
        val exception = MqttException(REASON_CODE_CLIENT_EXCEPTION.toInt(), cause)
        whenever(connectRetryTimePolicy.getConnRetryTimeSecs()).thenReturn(8)
        mqttExceptionHandlerImpl.handleException(exception, true)
        verify(runnableScheduler).scheduleNextConnectionCheck(8)
    }

    @Test
    fun `test exception with reason code 3`() {
        val exception = MqttException(REASON_CODE_BROKER_UNAVAILABLE.toInt())
        whenever(random.nextInt(SERVER_UNAVAILABLE_MAX_CONNECT_TIME)).thenReturn(3)
        mqttExceptionHandlerImpl.handleException(exception, true)
        verify(runnableScheduler).scheduleNextConnectionCheck(4 * 60L)
    }

    @Test
    fun `test exception with reason code 4`() {
        val exception = MqttException(REASON_CODE_FAILED_AUTHENTICATION.toInt())
        mqttExceptionHandlerImpl.handleException(exception, true)
        verify(runnableScheduler).scheduleAuthFailureRunnable()
    }

    @Test
    fun `test exception with reason code 5`() {
        val exception = MqttException(REASON_CODE_NOT_AUTHORIZED.toInt())
        mqttExceptionHandlerImpl.handleException(exception, true)
        verify(runnableScheduler).scheduleAuthFailureRunnable()
    }

    @Test
    fun `test exception with reason code 6`() {
        val exception = MqttException(REASON_CODE_UNEXPECTED_ERROR.toInt())
        whenever(connectRetryTimePolicy.getConnRetryTimeSecs()).thenReturn(20)
        mqttExceptionHandlerImpl.handleException(exception, true)
        verify(runnableScheduler).scheduleNextConnectionCheck(20)
    }

    @Test
    fun `test exception with reason code 32101 with reconnect=true`() {
        val exception = MqttException(REASON_CODE_CLIENT_ALREADY_DISCONNECTED.toInt())
        whenever(connectRetryTimePolicy.getConnRetryTimeSecs()).thenReturn(20)
        mqttExceptionHandlerImpl.handleException(exception, true)
        verify(runnableScheduler).connectMqtt(20 * 1000L)
    }

    @Test
    fun `test exception with reason code 32101 with reconnect=false`() {
        val exception = MqttException(REASON_CODE_CLIENT_ALREADY_DISCONNECTED.toInt())
        mqttExceptionHandlerImpl.handleException(exception, false)
        verifyZeroInteractions(runnableScheduler, connectRetryTimePolicy)
    }

    @Test
    fun `test exception with reason code 32102 with reconnect=true`() {
        val exception = MqttException(REASON_CODE_CLIENT_DISCONNECTING.toInt())
        mqttExceptionHandlerImpl.handleException(exception, true)
        verify(runnableScheduler).scheduleNextConnectionCheck(1)
    }

    @Test
    fun `test exception with reason code 32102 with reconnect=false`() {
        val exception = MqttException(REASON_CODE_CLIENT_DISCONNECTING.toInt())
        mqttExceptionHandlerImpl.handleException(exception, false)
        verifyZeroInteractions(runnableScheduler, connectRetryTimePolicy)
    }

    @Test
    fun `test exception with reason code 32104 with reconnect=true`() {
        val exception = MqttException(REASON_CODE_CLIENT_NOT_CONNECTED.toInt())
        mqttExceptionHandlerImpl.handleException(exception, true)
        verify(runnableScheduler).connectMqtt()
    }

    @Test
    fun `test exception with reason code 32104 with reconnect=false`() {
        val exception = MqttException(REASON_CODE_CLIENT_NOT_CONNECTED.toInt())
        mqttExceptionHandlerImpl.handleException(exception, false)
        verifyZeroInteractions(runnableScheduler, connectRetryTimePolicy)
    }

    @Test
    fun `test exception with reason code 32000 with reconnect=true`() {
        val exception = MqttException(REASON_CODE_CLIENT_TIMEOUT.toInt())
        mqttExceptionHandlerImpl.handleException(exception, true)
        verify(runnableScheduler).connectMqtt()
    }

    @Test
    fun `test exception with reason code 32000 with reconnect=false`() {
        val exception = MqttException(REASON_CODE_CLIENT_TIMEOUT.toInt())
        mqttExceptionHandlerImpl.handleException(exception, false)
        verifyZeroInteractions(runnableScheduler, connectRetryTimePolicy)
    }

    @Test
    fun `test exception with reason code 32110`() {
        val exception = MqttException(REASON_CODE_CONNECT_IN_PROGRESS.toInt())
        mqttExceptionHandlerImpl.handleException(exception, true)
        verifyZeroInteractions(runnableScheduler, connectRetryTimePolicy)
    }

    @Test
    fun `test exception with reason code 32103`() {
        val exception = MqttException(REASON_CODE_SERVER_CONNECT_ERROR.toInt())
        whenever(connectRetryTimePolicy.getConnRetryTimeSecs()).thenReturn(20)
        mqttExceptionHandlerImpl.handleException(exception, true)
        verify(runnableScheduler).scheduleNextConnectionCheck(20)
    }

    @Test
    fun `test exception with reason code 32109 with reconnect=true`() {
        val exception = MqttException(REASON_CODE_CONNECTION_LOST.toInt())
        whenever(connectRetryTimePolicy.getConnRetryTimeSecs()).thenReturn(20)
        mqttExceptionHandlerImpl.handleException(exception, true)
        verify(runnableScheduler).scheduleNextConnectionCheck(20)
    }

    @Test
    fun `test exception with reason code 32109 with reconnect=false`() {
        val exception = MqttException(REASON_CODE_CONNECTION_LOST.toInt())
        mqttExceptionHandlerImpl.handleException(exception, false)
        verifyZeroInteractions(runnableScheduler, connectRetryTimePolicy)
    }

    @Test
    fun `test exception with reason code 32202`() {
        val exception = MqttException(REASON_CODE_MAX_INFLIGHT.toInt())
        mqttExceptionHandlerImpl.handleException(exception, true)
        verifyZeroInteractions(runnableScheduler, connectRetryTimePolicy)
    }

    @Test
    fun `test exception with reason code 32111`() {
        val exception = MqttException(REASON_CODE_CLIENT_CLOSED.toInt())
        mqttExceptionHandlerImpl.handleException(exception, true)
        verifyZeroInteractions(runnableScheduler, connectRetryTimePolicy)
    }

    @Test
    fun `test exception with reason code 32100`() {
        val exception = MqttException(REASON_CODE_CLIENT_CONNECTED.toInt())
        mqttExceptionHandlerImpl.handleException(exception, true)
        verifyZeroInteractions(runnableScheduler, connectRetryTimePolicy)
    }

    @Test
    fun `test exception with reason code 32107`() {
        val exception = MqttException(REASON_CODE_CLIENT_DISCONNECT_PROHIBITED.toInt())
        mqttExceptionHandlerImpl.handleException(exception, true)
        verifyZeroInteractions(runnableScheduler, connectRetryTimePolicy)
    }

    @Test
    fun `test exception with reason code 2`() {
        val exception = MqttException(REASON_CODE_INVALID_CLIENT_ID.toInt())
        mqttExceptionHandlerImpl.handleException(exception, true)
        verifyZeroInteractions(runnableScheduler, connectRetryTimePolicy)
    }

    @Test
    fun `test exception with reason code 32108`() {
        val exception = MqttException(REASON_CODE_INVALID_MESSAGE.toInt())
        mqttExceptionHandlerImpl.handleException(exception, true)
        verifyZeroInteractions(runnableScheduler, connectRetryTimePolicy)
    }

    @Test
    fun `test exception with reason code 1`() {
        val exception = MqttException(REASON_CODE_INVALID_PROTOCOL_VERSION.toInt())
        mqttExceptionHandlerImpl.handleException(exception, true)
        verifyZeroInteractions(runnableScheduler, connectRetryTimePolicy)
    }

    @Test
    fun `test exception with reason code 32001`() {
        val exception = MqttException(REASON_CODE_NO_MESSAGE_IDS_AVAILABLE.toInt())
        mqttExceptionHandlerImpl.handleException(exception, true)
        verifyZeroInteractions(runnableScheduler, connectRetryTimePolicy)
    }

    @Test
    fun `test exception with reason code 32105`() {
        val exception = MqttException(REASON_CODE_SOCKET_FACTORY_MISMATCH.toInt())
        mqttExceptionHandlerImpl.handleException(exception, true)
        verifyZeroInteractions(runnableScheduler, connectRetryTimePolicy)
    }

    @Test
    fun `test exception with reason code 32106`() {
        val exception = MqttException(REASON_CODE_SSL_CONFIG_ERROR.toInt())
        mqttExceptionHandlerImpl.handleException(exception, true)
        verifyZeroInteractions(runnableScheduler, connectRetryTimePolicy)
    }

    @Test
    fun `test exception with reason code 32201`() {
        val exception = MqttException(REASON_CODE_TOKEN_INUSE.toInt())
        mqttExceptionHandlerImpl.handleException(exception, true)
        verifyZeroInteractions(runnableScheduler, connectRetryTimePolicy)
    }
}
