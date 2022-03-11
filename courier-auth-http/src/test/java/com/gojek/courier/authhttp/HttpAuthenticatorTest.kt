package com.gojek.courier.authhttp

import com.gojek.courier.authhttp.handler.ResponseHandler
import com.gojek.courier.authhttp.retry.AuthRetryPolicy
import com.gojek.courier.authhttp.service.ApiService
import com.gojek.courier.exception.AuthApiException
import com.gojek.courier.utils.Clock
import com.gojek.mqtt.event.EventHandler
import com.gojek.mqtt.event.MqttEvent.AuthenticatorAttemptEvent
import com.gojek.mqtt.event.MqttEvent.AuthenticatorSuccessEvent
import com.gojek.mqtt.model.MqttConnectOptions
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import okhttp3.ResponseBody
import org.junit.After
import org.junit.Test
import retrofit2.Call
import retrofit2.HttpException
import retrofit2.Response
import java.lang.RuntimeException
import kotlin.test.assertEquals


class HttpAuthenticatorTest {
    private val apiService = mock<ApiService>()
    private val apiUrl = "test_url"
    private val connectOptions = mock<MqttConnectOptions>()
    private val responseHandler = mock<ResponseHandler>()
    private val authRetryPolicy = mock<AuthRetryPolicy>()
    private val eventHandler = mock<EventHandler>()
    private val clock = mock<Clock>()
    private val headerMap = mapOf("Test-Header" to "test-value")

    private val httpAuthenticator = HttpAuthenticator(
        apiService = apiService,
        apiUrl = apiUrl,
        responseHandler = responseHandler,
        authRetryPolicy = authRetryPolicy,
        clock = clock,
        eventHandler = eventHandler,
        headerMap = headerMap
    )

    @Test
    fun `test authenticate with force refresh as false and password as non empty`() {
        whenever(connectOptions.password).thenReturn("test_password")
        val updatedConnectOptions = httpAuthenticator.authenticate(connectOptions, false)

        assertEquals(updatedConnectOptions, connectOptions)
        verify(connectOptions).password
    }

    @Test
    fun `test authenticate with force refresh as false and empty password`() {
        whenever(connectOptions.password).thenReturn("")
        val response = mock<ResponseBody>()
        val call = mock<Call<ResponseBody>>()
        val newConnectOptions = mock<MqttConnectOptions>()
        whenever(call.execute()).thenReturn(Response.success(response))
        whenever(apiService.authenticate(apiUrl, headerMap)).thenReturn(call)
        whenever(responseHandler.handleResponse(response, connectOptions)).thenReturn(newConnectOptions)
        whenever(clock.nanoTime()).thenReturn(1000*1000000L, 1010*1000000L)

        val updatedConnectOptions = httpAuthenticator.authenticate(connectOptions, false)

        assertEquals(updatedConnectOptions, newConnectOptions)
        verify(connectOptions).password
        verify(apiService).authenticate(apiUrl, headerMap)
        verify(responseHandler).handleResponse(response, connectOptions)
        verify(authRetryPolicy).reset()
        verify(eventHandler).onEvent(AuthenticatorAttemptEvent(false, connectOptions))
        verify(eventHandler).onEvent(AuthenticatorSuccessEvent(false, updatedConnectOptions, 10))
        verify(clock, times(2)).nanoTime()
    }

    @Test
    fun `test authenticate with force refresh as true`() {
        val response = mock<ResponseBody>()
        val call = mock<Call<ResponseBody>>()
        val newConnectOptions = mock<MqttConnectOptions>()
        whenever(call.execute()).thenReturn(Response.success(response))
        whenever(apiService.authenticate(apiUrl, headerMap)).thenReturn(call)
        whenever(responseHandler.handleResponse(response, connectOptions)).thenReturn(newConnectOptions)
        whenever(clock.nanoTime()).thenReturn(1000*1000000L, 1010*1000000L)

        val updatedConnectOptions = httpAuthenticator.authenticate(connectOptions, true)

        assertEquals(updatedConnectOptions, newConnectOptions)
        verify(apiService).authenticate(apiUrl, headerMap)
        verify(responseHandler).handleResponse(response, connectOptions)
        verify(authRetryPolicy).reset()
        verify(eventHandler).onEvent(AuthenticatorAttemptEvent(true, connectOptions))
        verify(eventHandler).onEvent(AuthenticatorSuccessEvent(true, updatedConnectOptions, 10))
        verify(clock, times(2)).nanoTime()
    }

    @Test
    fun `test authenticate with force refresh as true and api throwing exception`() {
        val response = mock<Response<ResponseBody>>()
        val call = mock<Call<ResponseBody>>()
        whenever(call.execute()).thenReturn(response)
        whenever(apiService.authenticate(apiUrl, headerMap)).thenReturn(call)
        whenever(response.isSuccessful).thenReturn(false)
        whenever(authRetryPolicy.getRetrySeconds(any<HttpException>())).thenReturn(5)
        whenever(clock.nanoTime()).thenReturn(1000*1000000L)

        try {
            httpAuthenticator.authenticate(connectOptions, true)
        } catch (ex: AuthApiException) {
            assertEquals(ex.nextRetrySeconds, 5)
            verify(apiService).authenticate(apiUrl, headerMap)
            verify(authRetryPolicy).getRetrySeconds(any<HttpException>())
            verify(eventHandler).onEvent(AuthenticatorAttemptEvent(true, connectOptions))
            verify(clock).nanoTime()
        }
    }

    @Test
    fun `test authenticate with force refresh as true and api call failure`() {
        whenever(apiService.authenticate(apiUrl, headerMap)).thenThrow(RuntimeException("Test"))
        whenever(authRetryPolicy.getRetrySeconds(any<RuntimeException>())).thenReturn(5)
        whenever(clock.nanoTime()).thenReturn(1000*1000000L)

        try {
            httpAuthenticator.authenticate(connectOptions, true)
        } catch (ex: AuthApiException) {
            assertEquals(ex.nextRetrySeconds, 5)
            verify(apiService).authenticate(apiUrl, headerMap)
            verify(authRetryPolicy).getRetrySeconds(any<RuntimeException>())
            verify(eventHandler).onEvent(AuthenticatorAttemptEvent(true, connectOptions))
            verify(clock).nanoTime()
        }
    }

    @After
    fun teardown() {
        verifyNoMoreInteractions(eventHandler, apiService, responseHandler, authRetryPolicy, connectOptions, clock)
    }
}