package com.gojek.mqtt.client

import com.gojek.courier.Message
import com.gojek.courier.QoS
import com.gojek.courier.callback.SendMessageCallback
import com.gojek.mqtt.client.internal.MqttClientInternal
import com.gojek.mqtt.client.listener.MessageListener
import com.gojek.mqtt.client.model.ConnectionState
import com.gojek.mqtt.model.MqttConnectOptions
import com.gojek.mqtt.model.MqttPacket
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class MqttCourierClientTest {
    private val mqttClientInternal = mock<MqttClientInternal>()
    private val mqttCourierClient = MqttCourierClient(
        mqttClientInternal
    )

    @Test
    fun `test connect`() {
        val connectOptions = mock<MqttConnectOptions>()
        mqttCourierClient.connect(connectOptions)
        verify(mqttClientInternal).connect(connectOptions)
    }

    @Test
    fun `test getCurrentState`() {
        val connectionState = mock<ConnectionState>()
        whenever(mqttClientInternal.getCurrentState()).thenReturn(connectionState)
        val currentState = mqttCourierClient.getCurrentState()
        assertEquals(connectionState, currentState)
        verify(mqttClientInternal).getCurrentState()
    }

    @Test
    fun `test disconnect with clearState=false`() {
        val clearState = false
        mqttCourierClient.disconnect(clearState)
        verify(mqttClientInternal).disconnect()
    }

    @Test
    fun `test disconnect with clearState=true`() {
        val clearState = true
        mqttCourierClient.disconnect(clearState)
        verify(mqttClientInternal).destroy()
    }

    @Test
    fun `test reconnect`() {
        mqttCourierClient.reconnect()
        verify(mqttClientInternal).reconnect()
    }

    @Test
    fun `test subscribe`() {
        val topic1 = Pair("test/topic1", QoS.ONE)
        val topic2 = Pair("test/topic2", QoS.ZERO)
        mqttCourierClient.subscribe(topic1, topic2)
        verify(mqttClientInternal).subscribe(topic1, topic2)
    }

    @Test
    fun `test unsubscribe`() {
        val topic1 = "test/topic1"
        val topic2 = "test/topic2"
        mqttCourierClient.unsubscribe(topic1, topic2)
        verify(mqttClientInternal).unsubscribe(topic1, topic2)
    }

    @Test
    fun `test send`() {
        val message = mock<Message.Bytes>()
        val callback = mock<SendMessageCallback>()
        val byteArray = ByteArray(10)
        whenever(message.value).thenReturn(byteArray)
        val topic = "test/topic"
        val qos = QoS.ZERO
        mqttCourierClient.send(message, topic, qos, callback)
        val argumentCaptor = argumentCaptor<MqttPacket>()
        verify(mqttClientInternal).send(argumentCaptor.capture(), eq(callback))
        assertEquals(argumentCaptor.lastValue.message, byteArray)
        assertEquals(argumentCaptor.lastValue.topic, topic)
        assertEquals(argumentCaptor.lastValue.qos, qos)
    }

    @Test
    fun `test addMessageListener`() {
        val topic = "test-topic"
        val listener = mock<MessageListener>()
        mqttCourierClient.addMessageListener(topic, listener)
        verify(mqttClientInternal).addMessageListener(topic, listener)
    }

    @Test
    fun `test removeMessageListener`() {
        val topic = "test-topic"
        val listener = mock<MessageListener>()
        mqttCourierClient.removeMessageListener(topic, listener)
        verify(mqttClientInternal).removeMessageListener(topic, listener)
    }

    @Test
    fun `test addGlobalMessageListener`() {
        val listener = mock<MessageListener>()
        mqttCourierClient.addGlobalMessageListener(listener)
        verify(mqttClientInternal).addGlobalMessageListener(listener)
    }
}
