package com.gojek.mqtt.exception

import com.gojek.courier.exception.AuthApiException
import org.eclipse.paho.client.mqttv3.MqttException

data class CourierException(
    val reasonCode: Int = -1,
    val type: String? = null,
    override val message: String? = null,
    override val cause: Throwable? = null
) : Throwable(message, cause)

internal fun Throwable?.toCourierException(): CourierException {
    return if (this == null) {
        CourierException()
    } else if (this is MqttException) {
        CourierException(
            type = this::class.java.simpleName,
            reasonCode = reasonCode,
            message = cause?.message ?: message,
            cause = cause?.cause ?: cause
        )
    } else if (this is AuthApiException) {
        CourierException(
            type = this::class.java.simpleName,
            reasonCode = reasonCode,
            message = message,
            cause = cause
        )
    } else {
        CourierException(
            type = this::class.java.simpleName,
            message = message,
            cause = cause
        )
    }
}
