package com.gojek.mqtt.exception

import com.gojek.courier.exception.AuthApiException
import org.eclipse.paho.client.mqtt.MqttException

data class CourierException(
    val reasonCode: Int = -1,
    override val message: String? = null,
    override val cause: Throwable? = null
) : Throwable(message, cause)

internal fun Throwable?.toCourierException(): CourierException {
    return if (this == null) {
        CourierException()
    } else if (this is MqttException) {
        CourierException(
            reasonCode = reasonCode,
            message = message,
            cause = cause
        )
    } else if (this is AuthApiException) {
        CourierException(
            reasonCode = reasonCode,
            message = message,
            cause = cause
        )
    } else {
        CourierException(
            message = message,
            cause = cause
        )
    }
}
