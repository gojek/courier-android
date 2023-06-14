package com.gojek.courier.callback

object NoOpSendMessageCallback : SendMessageCallback {
    override fun onMessageSendTrigger() {
        // no-op
    }

    override fun onMessageWrittenOnSocket() {
        // no-op
    }

    override fun onMessageSendSuccess() {
        // no-op
    }

    override fun onMessageSendFailure(error: Throwable) {
        // no-op
    }
}
