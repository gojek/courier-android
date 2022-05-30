package com.gojek.keepalive.sharedpref

import android.content.SharedPreferences
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
class CourierSharedPreferencesImplTest {
    private val sharedPreferences = mock<SharedPreferences>()

    private val courierSharedPreferences = CourierSharedPreferencesImpl(sharedPreferences)

    @Test
    fun `test has key should check the key in shared pref`() {
        val key = "some-test-key"
        whenever(sharedPreferences.contains(key)).thenReturn(true, false)

        assertTrue(courierSharedPreferences.has(key))
        assertFalse(courierSharedPreferences.has(key))

        verify(sharedPreferences, times(2)).contains(key)
    }

    @Test
    fun `test put integer should put integer in shared pref`() {
        val key = "some-test-key"
        val value = 10
        val mockedEditor = mock<SharedPreferences.Editor>()
        whenever(sharedPreferences.edit()).thenReturn(mockedEditor)
        whenever(mockedEditor.putInt(key, value)).thenReturn(mockedEditor)

        courierSharedPreferences.put(key, value)

        verify(sharedPreferences).edit()
        verify(mockedEditor).putInt(key, value)
        verify(mockedEditor).apply()
    }

    @Test
    fun `test put long should put long in shared pref`() {
        val key = "some-test-key"
        val value = 10L
        val mockedEditor = mock<SharedPreferences.Editor>()
        whenever(sharedPreferences.edit()).thenReturn(mockedEditor)
        whenever(mockedEditor.putLong(key, value)).thenReturn(mockedEditor)

        courierSharedPreferences.put(key, value)

        verify(sharedPreferences).edit()
        verify(mockedEditor).putLong(key, value)
        verify(mockedEditor).apply()
    }

    @Test
    fun `test put float should put float in shared pref`() {
        val key = "some-test-key"
        val value = 10f
        val mockedEditor = mock<SharedPreferences.Editor>()
        whenever(sharedPreferences.edit()).thenReturn(mockedEditor)
        whenever(mockedEditor.putFloat(key, value)).thenReturn(mockedEditor)

        courierSharedPreferences.put(key, value)

        verify(sharedPreferences).edit()
        verify(mockedEditor).putFloat(key, value)
        verify(mockedEditor).apply()
    }

    @Test
    fun `test put string should put string in shared pref`() {
        val key = "some-test-key"
        val value = "10"
        val mockedEditor = mock<SharedPreferences.Editor>()
        whenever(sharedPreferences.edit()).thenReturn(mockedEditor)
        whenever(mockedEditor.putString(key, value)).thenReturn(mockedEditor)

        courierSharedPreferences.put(key, value)

        verify(sharedPreferences).edit()
        verify(mockedEditor).putString(key, value)
        verify(mockedEditor).apply()
    }

    @Test
    fun `test put boolean should put boolean in shared pref`() {
        val key = "some-test-key"
        val value = false
        val mockedEditor = mock<SharedPreferences.Editor>()
        whenever(sharedPreferences.edit()).thenReturn(mockedEditor)
        whenever(mockedEditor.putBoolean(key, value)).thenReturn(mockedEditor)

        courierSharedPreferences.put(key, value)

        verify(sharedPreferences).edit()
        verify(mockedEditor).putBoolean(key, value)
        verify(mockedEditor).apply()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `test put incorrect type should throw exception`() {
        val key = "some-test-key"
        val value = Any()

        courierSharedPreferences.put(key, value)
    }

    @Test
    fun `test get integer should get integer from shared pref`() {
        val key = "some-test-key"
        val default = 10
        val value = 20
        whenever(sharedPreferences.getInt(key, default)).thenReturn(value)

        assertEquals(value, courierSharedPreferences.get(key, default))

        verify(sharedPreferences).getInt(key, default)
    }

    @Test
    fun `test get float should get float from shared pref`() {
        val key = "some-test-key"
        val default = 10f
        val value = 20f
        whenever(sharedPreferences.getFloat(key, default)).thenReturn(value)

        assertEquals(value, courierSharedPreferences.get(key, default))

        verify(sharedPreferences).getFloat(key, default)
    }

    @Test
    fun `test get string should get string from shared pref`() {
        val key = "some-test-key"
        val default = "10"
        val value = "20"
        whenever(sharedPreferences.getString(key, default)).thenReturn(value)

        assertEquals(value, courierSharedPreferences.get(key, default))

        verify(sharedPreferences).getString(key, default)
    }

    @Test
    fun `test get long should get long from shared pref`() {
        val key = "some-test-key"
        val default = 10L
        val value = 20L
        whenever(sharedPreferences.getLong(key, default)).thenReturn(value)

        assertEquals(value, courierSharedPreferences.get(key, default))

        verify(sharedPreferences).getLong(key, default)
    }

    @Test
    fun `test get boolean should get boolean from shared pref`() {
        val key = "some-test-key"
        val default = false
        val value = true
        whenever(sharedPreferences.getBoolean(key, default)).thenReturn(value)

        assertTrue(courierSharedPreferences.get(key, default))

        verify(sharedPreferences).getBoolean(key, default)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `test get incorrect type should throw exception`() {
        val key = "some-test-key"
        val default = Any()

        courierSharedPreferences.get(key, default)
    }

    @Test
    fun `test remove key should remove the key from shared pref`() {
        val key = "some-test-key"
        val mockedEditor = mock<SharedPreferences.Editor>()
        whenever(sharedPreferences.edit()).thenReturn(mockedEditor)
        whenever(mockedEditor.remove(key)).thenReturn(mockedEditor)

        courierSharedPreferences.remove(key)

        verify(sharedPreferences).edit()
        verify(mockedEditor).remove(key)
        verify(mockedEditor).apply()
    }

    @Test
    fun `test clear should clear the shared pref`() {
        val mockedEditor = mock<SharedPreferences.Editor>()
        whenever(sharedPreferences.edit()).thenReturn(mockedEditor)
        whenever(mockedEditor.clear()).thenReturn(mockedEditor)

        courierSharedPreferences.clear()

        verify(sharedPreferences).edit()
        verify(mockedEditor).clear()
        verify(mockedEditor).apply()
    }
}
