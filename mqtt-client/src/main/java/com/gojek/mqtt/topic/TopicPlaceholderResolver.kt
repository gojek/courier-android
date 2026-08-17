package com.gojek.mqtt.topic

import org.eclipse.paho.client.mqttv3.MqttConnectOptions

internal class TopicPlaceholderResolver {
    fun resolve(topic: String, options: MqttConnectOptions, clientId: String): String {
        if (!topic.contains(PLACEHOLDER_PREFIX)) {
            return topic
        }
        var resolvedTopic = topic
        if (resolvedTopic.contains(USERNAME_PLACEHOLDER)) {
            resolvedTopic = resolvedTopic.replace(USERNAME_PLACEHOLDER, options.userName)
        }
        if (resolvedTopic.contains(CLIENT_ID_PLACEHOLDER)) {
            resolvedTopic = resolvedTopic.replace(CLIENT_ID_PLACEHOLDER, clientId)
        }
        return resolvedTopic
    }

    companion object {
        private const val PLACEHOLDER_PREFIX = "%"
        private const val USERNAME_PLACEHOLDER = "%u"
        private const val CLIENT_ID_PLACEHOLDER = "%c"
    }
}
