package com.gojek.mqtt.subscription

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import androidx.annotation.VisibleForTesting
import com.gojek.courier.QoS
import com.gojek.courier.extensions.toImmutableMap
import com.gojek.courier.extensions.toImmutableSet

internal class PersistableSubscriptionStore(context: Context) : SubscriptionStore {
    private lateinit var state: State
    private val persistence = Persistence(context)
    private val listener = object : SubscriptionStoreListener {
        override fun onTopicsUnsubscribed(topics: Set<String>) {
            onTopicsUnsubscribedInternal(topics)
        }

        override fun onInvalidTopicsSubscribeFailure(topicMap: Map<String, QoS>) {
            state = state.copy(
                subscriptionTopics = state.subscriptionTopics - topicMap.keys,
                pendingUnsubscribeTopics = state.pendingUnsubscribeTopics
            )
        }

        override fun onInvalidTopicsUnsubscribeFailure(topics: Set<String>) {
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
        val addedTopics = topicMap - state.subscriptionTopics.keys
        state = state.copy(
            subscriptionTopics = state.subscriptionTopics + topicMap,
            pendingUnsubscribeTopics = state.pendingUnsubscribeTopics - topicMap.keys
        )
        persistence.put(PREF_KEY_PENDING_UNSUBSCRIBES, state.pendingUnsubscribeTopics)
        return addedTopics
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

internal class Persistence(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("SubscriptionStorePrefs", MODE_PRIVATE)

    fun get(key: String, default: Set<String>): Set<String> {
        return sharedPreferences.getStringSet(key, default)!!
    }

    fun put(key: String, value: Set<String>) {
        sharedPreferences.edit().putStringSet(key, value).apply()
    }
}

private const val PREF_KEY_PENDING_UNSUBSCRIBES = "PendingUnsubscribes"
