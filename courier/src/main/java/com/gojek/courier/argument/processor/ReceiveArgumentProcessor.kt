package com.gojek.courier.argument.processor

internal class ReceiveArgumentProcessor(
    private val pathMap: Map<String, Int>,
    private val topic: String
): ArgumentProcessor() {
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
}