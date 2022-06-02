package com.gojek.courier.authhttp.retry

interface AuthRetryPolicy {
    fun getRetrySeconds(error: Throwable): Long
    fun reset()
}

internal class DefaultAuthRetryPolicy : AuthRetryPolicy {
    override fun getRetrySeconds(error: Throwable): Long {
        return -1
    }

    override fun reset() {
    }
}
