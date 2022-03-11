package com.gojek.courier.annotation

import com.gojek.courier.QoS

@MustBeDocumented
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER
)
@kotlin.annotation.Retention(AnnotationRetention.RUNTIME)
annotation class Send(
    val topic: String,
    val qos: QoS = QoS.ZERO
) 