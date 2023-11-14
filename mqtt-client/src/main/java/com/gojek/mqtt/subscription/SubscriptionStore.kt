package com.gojek.mqtt.subscription

import com.gojek.courier.QoS

internal interface SubscriptionStore {
    fun getSubscribeTopics(): Map<String, QoS>
    fun getUnsubscribeTopics(cleanSession: Boolean): Set<String>
    fun subscribeTopics(topicMap: Map<String, QoS>): Map<String, QoS>
    fun unsubscribeTopics(topics: List<String>): Set<String>
    fun getListener(): SubscriptionStoreListener
    fun clear()
}

internal interface SubscriptionStoreListener {
    fun onTopicsSubscribed(topicMap: Map<String, QoS>) = Unit
    fun onInvalidTopicsSubscribeFailure(topicMap: Map<String, QoS>) = Unit
    fun onTopicsUnsubscribed(topics: Set<String>) = Unit
    fun onInvalidTopicsUnsubscribeFailure(topics: Set<String>) = Unit
}
