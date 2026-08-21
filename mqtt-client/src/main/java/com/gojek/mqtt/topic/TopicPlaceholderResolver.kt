package com.gojek.mqtt.topic

import com.gojek.courier.logging.ILogger
import org.eclipse.paho.client.mqttv3.MqttConnectOptions

internal class TopicPlaceholderResolver(private val logger: ILogger) {
    fun resolve(topic: String, options: MqttConnectOptions, clientId: String): String {
        if (!topic.contains(PLACEHOLDER_PREFIX)) {
            return topic
        }
        var resolvedTopic = topic

        resolvedTopic = SPLIT_PLACEHOLDER_REGEX.replace(resolvedTopic) { matchResult ->
            val placeholder = matchResult.groupValues[1]
            val delimiter = matchResult.groupValues[2]
            val partIndex = matchResult.groupValues[3].toInt()
            val parts = placeholder.placeholderValue(options, clientId).split(delimiter)
            require(partIndex in parts.indices) {
                "Invalid part index $partIndex for placeholder '$placeholder' split by " +
                    "'$delimiter': only ${parts.size} part(s) available"
            }
            parts[partIndex]
        }

        if (resolvedTopic.contains(USERNAME_PLACEHOLDER)) {
            resolvedTopic = resolvedTopic.replace(USERNAME_PLACEHOLDER, options.userName)
        }
        if (resolvedTopic.contains(CLIENT_ID_PLACEHOLDER)) {
            resolvedTopic = resolvedTopic.replace(CLIENT_ID_PLACEHOLDER, clientId)
        }
        return resolvedTopic.also {
            logger.d("TopicResolver", "$topic is resolved to $it, username: ${options.userName}, clientId: $clientId")
        }
    }

    private fun String.placeholderValue(options: MqttConnectOptions, clientId: String): String {
        return when (this) {
            USERNAME_PLACEHOLDER -> options.userName
            CLIENT_ID_PLACEHOLDER -> clientId
            else -> throw IllegalArgumentException("Unsupported placeholder: $this")
        }
    }

    companion object {
        private const val PLACEHOLDER_PREFIX = "%"
        private const val USERNAME_PLACEHOLDER = "%u"
        private const val CLIENT_ID_PLACEHOLDER = "%c"

        // Matches split placeholders like (%c,:,2) -> capture the placeholder (%c/%u), the
        // delimiter (any run of characters other than ',' and ')') and the 1-based part index.
        private val SPLIT_PLACEHOLDER_REGEX = Regex("""\((%[a-zA-Z]),([^,)]+),(\d+)\)""")
    }
}
