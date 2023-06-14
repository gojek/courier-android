package com.gojek.courier.argument.processor

import com.gojek.courier.callback.NoOpSendMessageCallback
import com.gojek.courier.callback.SendMessageCallback

internal class SendArgumentProcessor(
    private val pathMap: Map<String, Int>,
    private val topic: String,
    private val dataParameterIndex: Int,
    private val callbackIndex: Int
) : ArgumentProcessor() {
    private var parsedTopic = topic

    override fun inject(args: Array<Any>) {
        parsedTopic = topic
        for (entry in pathMap) {
            parsedTopic = parsedTopic.replace("{${entry.key}}", args[entry.value].toString())
        }
    }

    fun getTopic(): String {
        return parsedTopic
    }

    fun getDataArgument(args: Array<Any>): Any {
        return args[dataParameterIndex]
    }

    fun getCallbackArgument(args: Array<Any>): SendMessageCallback {
        return if (callbackIndex == -1) {
            NoOpSendMessageCallback
        } else {
            args[callbackIndex] as SendMessageCallback
        }
    }
}
