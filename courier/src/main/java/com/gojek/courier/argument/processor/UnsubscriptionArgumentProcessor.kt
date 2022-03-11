package com.gojek.courier.argument.processor

internal class UnsubscriptionArgumentProcessor(
    private val pathMap: Map<String, Int>,
    private val topics: Array<String>
): ArgumentProcessor() {
    private var parsedTopics: MutableList<String> = mutableListOf()

    override fun inject(args: Array<Any>) {
        parsedTopics.clear()
        for (index in topics.indices) {
            var parsedTopic = topics[index]
            for (entry in pathMap) {
                parsedTopic = parsedTopic.replace("{${entry.key}}", args[entry.value].toString())
            }
            parsedTopics.add(parsedTopic)
        }
    }

    fun getTopics(): Array<String> {
        return parsedTopics.toTypedArray()
    }
}