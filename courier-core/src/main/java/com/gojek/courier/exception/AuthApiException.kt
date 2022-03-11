package com.gojek.courier.exception

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY)
data class AuthApiException(
    val reasonCode: Int = -1,
    val nextRetrySeconds: Long,
    val failureCause: Throwable
): Exception(failureCause)