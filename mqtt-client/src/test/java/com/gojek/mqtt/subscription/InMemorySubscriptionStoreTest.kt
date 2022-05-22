package com.gojek.mqtt.subscription

import com.gojek.courier.QoS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class InMemorySubscriptionStoreTest {

    private val subscriptionStore = InMemorySubscriptionStore()

    @Test
    fun testSubscriptionStore() {
        assertTrue(subscriptionStore.getSubscribeTopics().isEmpty())

        val topic1 = "topic1" to QoS.ONE
        val topic2 = "topic2" to QoS.ONE
        val topic3 = "topic3" to QoS.ONE
        val topic4 = "topic4" to QoS.ONE
        val topic5 = "topic5" to QoS.ONE

        subscriptionStore.subscribeTopics(mapOf(topic1, topic2, topic3))
        assertEquals(subscriptionStore.getSubscribeTopics().size, 3)
        assertEquals(subscriptionStore.getSubscribeTopics(), mapOf(topic1, topic2, topic3))
        assertEquals(subscriptionStore.getUnsubscribeTopics(false).size, 0)

        subscriptionStore.subscribeTopics(mapOf(topic3))
        assertEquals(subscriptionStore.getSubscribeTopics().size, 3)
        assertEquals(subscriptionStore.getSubscribeTopics(), mapOf(topic1, topic2, topic3))
        assertEquals(subscriptionStore.getUnsubscribeTopics(false).size, 0)

        subscriptionStore.unsubscribeTopics(listOf(topic2.first))
        assertEquals(subscriptionStore.getSubscribeTopics().size, 2)
        assertEquals(subscriptionStore.getSubscribeTopics(), mapOf(topic1, topic3))
        assertEquals(subscriptionStore.getUnsubscribeTopics(false).size, 0)

        subscriptionStore.unsubscribeTopics(listOf(topic4.first))
        assertEquals(subscriptionStore.getSubscribeTopics().size, 2)
        assertEquals(subscriptionStore.getSubscribeTopics(), mapOf(topic1, topic3))
        assertEquals(subscriptionStore.getUnsubscribeTopics(false).size, 0)

        subscriptionStore.subscribeTopics(mapOf(topic5))
        assertEquals(subscriptionStore.getSubscribeTopics().size, 3)
        assertEquals(subscriptionStore.getSubscribeTopics(), mapOf(topic1, topic5, topic3))
        assertEquals(subscriptionStore.getUnsubscribeTopics(true).size, 0)

        subscriptionStore.unsubscribeTopics(listOf(topic1.first, topic3.first))
        assertEquals(subscriptionStore.getSubscribeTopics().size, 1)
        assertEquals(subscriptionStore.getSubscribeTopics(), mapOf(topic5))
        assertEquals(subscriptionStore.getUnsubscribeTopics(true).size, 0)
    }
}
