package com.gojek.mqtt.subscription

import android.content.Context
import android.content.SharedPreferences
import com.gojek.courier.QoS
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class PersistableSubscriptionStoreV2Test {
    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var sharedPreferences: SharedPreferences

    @Mock
    private lateinit var editor: SharedPreferences.Editor

    private lateinit var subscriptionStore: PersistableSubscriptionStoreV2

    @Before
    fun setup() {
        whenever(sharedPreferences.getStringSet("PendingUnsubscribes", emptySet()))
            .thenReturn(emptySet())
        whenever(sharedPreferences.edit()).thenReturn(editor)
        whenever(editor.putStringSet(any(), any())).thenReturn(editor)
        whenever(context.getSharedPreferences("SubscriptionStorePrefs", Context.MODE_PRIVATE))
            .thenReturn(sharedPreferences)
        subscriptionStore = PersistableSubscriptionStoreV2(context)
    }

    @Test
    fun testRestoreState() {
        assertEquals(subscriptionStore.getSubscribeTopics().size, 0)
        assertEquals(subscriptionStore.getUnsubscribeTopics(false).size, 0)
        whenever(sharedPreferences.getStringSet("PendingUnsubscribes", emptySet()))
            .thenReturn(setOf("topic1", "topic2"))
        subscriptionStore.restoreState()
        assertEquals(subscriptionStore.getSubscribeTopics().size, 0)
        assertEquals(subscriptionStore.getUnsubscribeTopics(false), setOf("topic1", "topic2"))
        assertEquals(subscriptionStore.getUnsubscribeTopics(false).size, 2)
    }

    @Test
    fun testSubscriptionStore() {
        assertTrue(subscriptionStore.getSubscribeTopics().isEmpty())

        val topic1 = "topic1" to QoS.ONE
        val topic2 = "topic2" to QoS.ONE
        val topic3 = "topic3" to QoS.ONE
        val topic4 = "topic4" to QoS.ONE
        val topic5 = "topic5" to QoS.ONE

        // Test when topic1, topic2, topic3 are subscribed for the first time
        var topicMap = mapOf(topic1, topic2, topic3)
        var subscribeTopics = subscriptionStore.subscribeTopics(topicMap)
        assertEquals(topicMap, subscribeTopics)
        assertEquals(subscriptionStore.getSubscribeTopics().size, 3)
        assertEquals(subscriptionStore.getSubscribeTopics(), mapOf(topic1, topic2, topic3))
        assertEquals(subscriptionStore.getUnsubscribeTopics(false).size, 0)

        // Test subscribing topic3 when its already subscribed
        topicMap = mapOf(topic3)
        subscribeTopics = subscriptionStore.subscribeTopics(topicMap)
        assertEquals(topicMap, subscribeTopics)
        assertEquals(subscriptionStore.getSubscribeTopics().size, 3)
        assertEquals(subscriptionStore.getSubscribeTopics(), mapOf(topic1, topic2, topic3))
        assertEquals(subscriptionStore.getUnsubscribeTopics(false).size, 0)

        // Test unsubscribing topic2 when its subscribed
        var topics = listOf(topic2.first)
        var unsubscribeTopics = subscriptionStore.unsubscribeTopics(topics)
        assertEquals(topics, unsubscribeTopics.toList())
        assertEquals(subscriptionStore.getSubscribeTopics().size, 2)
        assertEquals(subscriptionStore.getSubscribeTopics(), mapOf(topic1, topic3))
        assertEquals(subscriptionStore.getUnsubscribeTopics(false).size, 1)
        assertEquals(subscriptionStore.getUnsubscribeTopics(false), setOf(topic2.first))

        // Test unsubscribing topic4 when its subscribed
        topics = listOf(topic4.first)
        unsubscribeTopics = subscriptionStore.unsubscribeTopics(topics)
        assertEquals(topics, unsubscribeTopics.toList())
        assertEquals(subscriptionStore.getSubscribeTopics().size, 2)
        assertEquals(subscriptionStore.getSubscribeTopics(), mapOf(topic1, topic3))
        assertEquals(subscriptionStore.getUnsubscribeTopics(false).size, 2)
        assertEquals(
            subscriptionStore.getUnsubscribeTopics(false),
            setOf(topic2.first, topic4.first)
        )

        // Test subscribing topic5 when its not subscribed
        topicMap = mapOf(topic5)
        subscribeTopics = subscriptionStore.subscribeTopics(topicMap)
        assertEquals(topicMap, subscribeTopics)
        assertEquals(subscriptionStore.getSubscribeTopics().size, 3)
        assertEquals(subscriptionStore.getSubscribeTopics(), mapOf(topic1, topic5, topic3))
        assertEquals(subscriptionStore.getUnsubscribeTopics(false).size, 2)
        assertEquals(
            subscriptionStore.getUnsubscribeTopics(false),
            setOf(topic2.first, topic4.first)
        )

        // Test notifying unsubscribe success of topic2
        subscriptionStore.getListener().onTopicsUnsubscribed(setOf(topic2.first))
        assertEquals(subscriptionStore.getSubscribeTopics().size, 3)
        assertEquals(subscriptionStore.getSubscribeTopics(), mapOf(topic1, topic5, topic3))
        assertEquals(subscriptionStore.getUnsubscribeTopics(false).size, 1)
        assertEquals(subscriptionStore.getUnsubscribeTopics(false), setOf(topic4.first))

        // Test notifying unsubscribe success of topic5
        subscriptionStore.getListener().onTopicsSubscribed(mapOf(topic5))
        assertEquals(subscriptionStore.getSubscribeTopics().size, 3)
        assertEquals(subscriptionStore.getSubscribeTopics(), mapOf(topic1, topic5, topic3))
        assertEquals(subscriptionStore.getUnsubscribeTopics(false).size, 1)
        assertEquals(subscriptionStore.getUnsubscribeTopics(false), setOf(topic4.first))

        // Test getUnsubscribeTopics with cleansession true
        subscriptionStore.getListener().onTopicsSubscribed(mapOf(topic5))
        assertEquals(subscriptionStore.getSubscribeTopics().size, 3)
        assertEquals(subscriptionStore.getSubscribeTopics(), mapOf(topic1, topic5, topic3))
        assertEquals(subscriptionStore.getUnsubscribeTopics(true).size, 0)
    }

    @Test
    fun testClear() {
        val topic1 = "topic1" to QoS.ONE
        val topic2 = "topic2" to QoS.ZERO
        subscriptionStore.subscribeTopics(mapOf(topic1, topic2))
        subscriptionStore.unsubscribeTopics(listOf("topic3", "topic4"))
        assertEquals(subscriptionStore.getSubscribeTopics().size, 2)
        assertEquals(subscriptionStore.getUnsubscribeTopics(false).size, 2)
        subscriptionStore.clear()
        assertEquals(subscriptionStore.getSubscribeTopics().size, 0)
        assertEquals(subscriptionStore.getUnsubscribeTopics(false).size, 0)
    }
}
