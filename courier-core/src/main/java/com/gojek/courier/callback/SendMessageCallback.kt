package com.gojek.courier.callback

interface SendMessageCallback {
    fun onMessageSendTrigger()
    fun onMessageWrittenOnSocket()
    fun onMessageSendSuccess()
    fun onMessageSendFailure(error: Throwable)
}
