package com.gojek.courier.authhttp

import com.gojek.courier.authhttp.handler.ResponseHandler
import com.gojek.courier.authhttp.retry.AuthRetryPolicy
import com.gojek.courier.authhttp.service.ApiService
import com.gojek.courier.exception.AuthApiException
import com.gojek.courier.extensions.fromNanosToMillis
import com.gojek.courier.utils.Clock
import com.gojek.mqtt.auth.Authenticator
import com.gojek.mqtt.event.EventHandler
import com.gojek.mqtt.event.MqttEvent.AuthenticatorAttemptEvent
import com.gojek.mqtt.event.MqttEvent.AuthenticatorSuccessEvent
import com.gojek.mqtt.model.MqttConnectOptions
import retrofit2.HttpException

internal class HttpAuthenticator(
    private val apiService: ApiService,
    private val apiUrl: String,
    private val responseHandler: ResponseHandler,
    private val authRetryPolicy: AuthRetryPolicy,
    private val eventHandler: EventHandler,
    private val clock: Clock = Clock(),
    private val headerMap: Map<String, String>
): Authenticator {

    override fun authenticate(
        connectOptions: MqttConnectOptions,
        forceRefresh: Boolean
    ): MqttConnectOptions {
        if (forceRefresh.not() && connectOptions.password.isNotEmpty()) {
            return connectOptions
        }
        try {
            val startTime = clock.nanoTime()
            eventHandler.onEvent(AuthenticatorAttemptEvent(forceRefresh, connectOptions))
            val response = apiService.authenticate(apiUrl, headerMap).execute()
            if (response.isSuccessful.not()) {
                throw HttpException(response)
            }
            authRetryPolicy.reset()
            val updatedConnectOptions = responseHandler.handleResponse(response.body()!!, connectOptions)
            eventHandler.onEvent(
                AuthenticatorSuccessEvent(
                    forceRefresh = forceRefresh,
                    connectOptions = updatedConnectOptions,
                    timeTakenMillis = (clock.nanoTime() - startTime).fromNanosToMillis()
                )
            )
            return updatedConnectOptions
        } catch (th: Throwable) {
            throw parseException(th)
        }
    }

    private fun parseException(throwable: Throwable): AuthApiException {
        return if (throwable is HttpException) {
            AuthApiException(
                reasonCode = throwable.code(),
                nextRetrySeconds = authRetryPolicy.getRetrySeconds(throwable),
                failureCause = throwable
            )
        } else {
            AuthApiException(
                nextRetrySeconds = authRetryPolicy.getRetrySeconds(throwable),
                failureCause = throwable
            )
        }
    }
}