package com.gojek.mqtt.policies.subscriptionretry

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class SubscriptionRetryPolicyTest {
    private val subscriptionRetryConfig = mock<SubscriptionRetryConfig>()
    private val subscriptionRetryPolicy = SubscriptionRetryPolicy(subscriptionRetryConfig)

    @Test
    fun `test shouldRetry`() {
        whenever(subscriptionRetryConfig.maxRetryCount).thenReturn(3)

        assertEquals(subscriptionRetryPolicy.getRetryCount(), 0)
        assertTrue(subscriptionRetryPolicy.shouldRetry())
        assertEquals(subscriptionRetryPolicy.getRetryCount(), 1)
        assertTrue(subscriptionRetryPolicy.shouldRetry())
        assertEquals(subscriptionRetryPolicy.getRetryCount(), 2)
        assertTrue(subscriptionRetryPolicy.shouldRetry())
        assertEquals(subscriptionRetryPolicy.getRetryCount(), 3)
        assertFalse(subscriptionRetryPolicy.shouldRetry())
        assertEquals(subscriptionRetryPolicy.getRetryCount(), 4)
        assertFalse(subscriptionRetryPolicy.shouldRetry())
        assertEquals(subscriptionRetryPolicy.getRetryCount(), 5)

        subscriptionRetryPolicy.resetParams()
        assertEquals(subscriptionRetryPolicy.getRetryCount(), 0)
        assertTrue(subscriptionRetryPolicy.shouldRetry())
        assertEquals(subscriptionRetryPolicy.getRetryCount(), 1)
    }
}