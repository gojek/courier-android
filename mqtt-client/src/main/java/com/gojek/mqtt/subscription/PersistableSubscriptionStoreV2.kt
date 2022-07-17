package com.gojek.mqtt.subscription

import android.content.Context
import androidx.annotation.VisibleForTesting
import com.gojek.courier.QoS
import com.gojek.courier.extensions.toImmutableMap
import com.gojek.courier.extensions.toImmutableSet

internal class PersistableSubscriptionStoreV2(context: Context) : SubscriptionStore {
    private lateinit var state: State
    private val persistence = Persistence(context)
    private val listener = object : SubscriptionStoreListener {
        override fun onTopicsUnsubscribed(topics: Set<String>) {
            onTopicsUnsubscribedInternal(topics)
        }
    }

    private data class State(
        val subscriptionTopics: Map<String, QoS>,
        val pendingUnsubscribeTopics: Set<String>
    )

    init {
        restoreState()
    }

    @Synchronized
    override fun getSubscribeTopics(): Map<String, QoS> {
        return state.subscriptionTopics.toImmutableMap()
    }

    @Synchronized
    override fun getUnsubscribeTopics(cleanSession: Boolean): Set<String> {
        if (cleanSession) {
            persistence.put(PREF_KEY_PENDING_UNSUBSCRIBES, emptySet())
            state = state.copy(pendingUnsubscribeTopics = emptySet())
        }
        return state.pendingUnsubscribeTopics.toImmutableSet()
    }

    @Synchronized
    override fun subscribeTopics(topicMap: Map<String, QoS>): Map<String, QoS> {
        state = state.copy(
            subscriptionTopics = state.subscriptionTopics + topicMap,
            pendingUnsubscribeTopics = state.pendingUnsubscribeTopics - topicMap.keys
        )
        persistence.put(PREF_KEY_PENDING_UNSUBSCRIBES, state.pendingUnsubscribeTopics)
        return topicMap
    }

    @Synchronized
    override fun unsubscribeTopics(topics: List<String>): Set<String> {
        state = state.copy(
            subscriptionTopics = state.subscriptionTopics - topics,
            pendingUnsubscribeTopics = state.pendingUnsubscribeTopics + topics
        )
        persistence.put(PREF_KEY_PENDING_UNSUBSCRIBES, state.pendingUnsubscribeTopics)
        return topics.toSet()
    }

    override fun getListener(): SubscriptionStoreListener {
        return listener
    }

    @Synchronized
    override fun clear() {
        persistence.put(PREF_KEY_PENDING_UNSUBSCRIBES, emptySet())
        state = State(
            subscriptionTopics = emptyMap(),
            pendingUnsubscribeTopics = emptySet()
        )
    }

    @VisibleForTesting
    internal fun restoreState() {
        state = State(
            subscriptionTopics = emptyMap(),
            pendingUnsubscribeTopics = persistence.get(PREF_KEY_PENDING_UNSUBSCRIBES, emptySet())
        )
    }

    @Synchronized
    private fun onTopicsUnsubscribedInternal(topics: Set<String>) {
        state = state.copy(
            subscriptionTopics = state.subscriptionTopics,
            pendingUnsubscribeTopics = state.pendingUnsubscribeTopics - topics
        )
        persistence.put(PREF_KEY_PENDING_UNSUBSCRIBES, state.pendingUnsubscribeTopics)
    }
}

private const val PREF_KEY_PENDING_UNSUBSCRIBES = "PendingUnsubscribes"
