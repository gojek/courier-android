package com.gojek.mqtt.topic

import org.eclipse.paho.client.mqttv3.MqttConnectOptions

internal class TopicPlaceholderResolver {
    fun resolve(topic: String, options: MqttConnectOptions): String {
        if (!topic.contains(PLACEHOLDER_PREFIX)) {
            return topic
        }
        var resolvedTopic = topic
        if (resolvedTopic.contains(USERNAME_PLACEHOLDER)) {
            resolvedTopic = resolvedTopic.replace(USERNAME_PLACEHOLDER, options.userName)
        }
        return resolvedTopic
    }

    companion object {
        private const val PLACEHOLDER_PREFIX = "%"
        private const val USERNAME_PLACEHOLDER = "%username"
    }
}
