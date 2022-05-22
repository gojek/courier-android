package com.gojek.courier.authhttp

import com.gojek.courier.authhttp.handler.ResponseHandler
import com.gojek.courier.authhttp.retry.AuthRetryPolicy
import com.gojek.courier.authhttp.retry.DefaultAuthRetryPolicy
import com.gojek.courier.authhttp.service.ApiService
import com.gojek.mqtt.auth.Authenticator
import com.gojek.mqtt.event.EventHandler
import retrofit2.Retrofit

class HttpAuthenticatorFactory private constructor() {
    companion object {
        fun create(
            retrofit: Retrofit,
            apiUrl: String,
            responseHandler: ResponseHandler,
            eventHandler: EventHandler,
            authRetryPolicy: AuthRetryPolicy = DefaultAuthRetryPolicy(),
            headerMap: Map<String, String> = emptyMap()
        ): Authenticator {
            val apiService = retrofit.create(ApiService::class.java)
            return HttpAuthenticator(
                apiService = apiService,
                apiUrl = apiUrl,
                responseHandler = responseHandler,
                authRetryPolicy = authRetryPolicy,
                eventHandler = eventHandler,
                headerMap = headerMap
            )
        }
    }
}
