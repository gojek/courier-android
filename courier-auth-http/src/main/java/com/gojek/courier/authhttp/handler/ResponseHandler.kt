package com.gojek.courier.authhttp.handler

import com.gojek.mqtt.model.MqttConnectOptions
import okhttp3.ResponseBody

interface ResponseHandler {
    fun handleResponse(
        responseBody: ResponseBody,
        connectOptions: MqttConnectOptions
    ): MqttConnectOptions
}