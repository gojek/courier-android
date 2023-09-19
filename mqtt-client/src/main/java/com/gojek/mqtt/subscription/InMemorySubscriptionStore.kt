package com.gojek.mqtt.subscription

import com.gojek.courier.QoS

internal class InMemorySubscriptionStore : SubscriptionStore {
    private var state = State(mapOf())
    private val listener = object : SubscriptionStoreListener {
        override fun onInvalidTopicsSubscribeFailure(topicMap: Map<String, QoS>) {
            state = state.copy(
                subscriptionTopics = state.subscriptionTopics - topicMap.keys
            )
        }
    }

    private data class State(val subscriptionTopics: Map<String, QoS>)

    @Synchronized
    override fun getSubscribeTopics(): Map<String, QoS> {
        return HashMap(state.subscriptionTopics)
    }

    override fun getUnsubscribeTopics(cleanSession: Boolean): Set<String> {
        return emptySet()
    }

    @Synchronized
    override fun subscribeTopics(topicMap: Map<String, QoS>): Map<String, QoS> {
        val addedTopics = topicMap - state.subscriptionTopics.keys
        state = state.copy(
            subscriptionTopics = state.subscriptionTopics + topicMap
        )
        return addedTopics
    }

    @Synchronized
    override fun unsubscribeTopics(topics: List<String>): Set<String> {
        val removedTopics = state.subscriptionTopics.keys.intersect(topics)
        state = state.copy(
            subscriptionTopics = state.subscriptionTopics - topics
        )
        return removedTopics
    }

    override fun getListener(): SubscriptionStoreListener {
        return listener
    }

    override fun clear() {
        state = State(emptyMap())
    }
}
