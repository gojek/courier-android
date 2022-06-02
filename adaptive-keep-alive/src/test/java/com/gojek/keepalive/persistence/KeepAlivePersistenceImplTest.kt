package com.gojek.keepalive.persistence

import com.gojek.keepalive.model.KeepAlivePersistenceModel
import com.gojek.keepalive.sharedpref.CourierSharedPreferences
import com.google.gson.Gson
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class KeepAlivePersistenceImplTest {
    private val sharedPreferences = mock<CourierSharedPreferences>()
    private val gson = mock<Gson>()

    private val keepAlivePersistence = KeepAlivePersistenceImpl(sharedPreferences, gson)

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
        val default = ""
        val value = "actual-value"
        val keepAlivePersistenceModel = mock<KeepAlivePersistenceModel>()
        whenever(sharedPreferences.get(key, default)).thenReturn(value)
        whenever(gson.fromJson(value, KeepAlivePersistenceModel::class.java)).thenReturn(keepAlivePersistenceModel)

        assertEquals(keepAlivePersistenceModel, keepAlivePersistence.get(key))

        verify(sharedPreferences).get(key, default)
        verify(gson).fromJson(value, KeepAlivePersistenceModel::class.java)
    }

    @Test
    fun `test put key should put the key in shared pref`() {
        val key = "some-test-key"
        val value = "actual-value"
        val keepAlivePersistenceModel = mock<KeepAlivePersistenceModel>()
        whenever(gson.toJson(keepAlivePersistenceModel)).thenReturn(value)

        keepAlivePersistence.put(key, keepAlivePersistenceModel)

        verify(gson).toJson(keepAlivePersistenceModel)
        verify(sharedPreferences).put(key, value)
    }

    @Test
    fun `test remove key should remove the key from shared pref`() {
        val key = "some-test-key"

        keepAlivePersistence.remove(key)

        verify(sharedPreferences).remove(key)
    }
}
