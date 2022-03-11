package com.gojek.keepalive.persistence

import com.gojek.keepalive.sharedpref.CourierSharedPreferences
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(MockitoJUnitRunner::class)
class KeepAlivePersistenceImplTest {
    private val sharedPreferences = mock<CourierSharedPreferences>()

    private val keepAlivePersistence = KeepAlivePersistenceImpl(sharedPreferences)

    @Test
    fun `test has key should check the key in shared pref`() {
        val key = "some-test-key"
        whenever(sharedPreferences.has(key)).thenReturn(true, false)

        assertTrue(keepAlivePersistence.has(key))
        assertFalse(keepAlivePersistence.has(key))

        verify(sharedPreferences, times(2)).has(key)
    }

    @Test
    fun `test get key should get the key from shared pref`() {
        val key = "some-test-key"
        val default = 10
        val value = 20
        whenever(sharedPreferences.get(key, default)).thenReturn(value)

        assertEquals(value, keepAlivePersistence.get(key, default))

        verify(sharedPreferences).get(key, default)
    }

    @Test
    fun `test put key should put the key in shared pref`() {
        val key = "some-test-key"
        val value = 10

        keepAlivePersistence.put(key, value)

        verify(sharedPreferences).put(key, value)
    }

    @Test
    fun `test remove key should remove the key from shared pref`() {
        val key = "some-test-key"

        keepAlivePersistence.remove(key)

        verify(sharedPreferences).remove(key)
    }
}