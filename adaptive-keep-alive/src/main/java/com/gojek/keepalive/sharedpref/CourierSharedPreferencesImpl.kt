package com.gojek.keepalive.sharedpref

import android.content.SharedPreferences as AndroidSharedPreferences

@Suppress("UNCHECKED_CAST")
internal class CourierSharedPreferencesImpl(
    private val sharedPreferences: AndroidSharedPreferences
): CourierSharedPreferences {
    override fun has(key: String): Boolean {
        return sharedPreferences.contains(key)
    }

    override fun <T> get(key: String, default: T): T {
        return when (default) {
            is Boolean -> {
                sharedPreferences.getBoolean(key, default) as T
            }
            is String -> {
                sharedPreferences.getString(key, default) as T
            }
            is Int -> {
                sharedPreferences.getInt(key, default) as T
            }
            is Float -> {
                sharedPreferences.getFloat(key, default) as T
            }
            is Long -> {
                sharedPreferences.getLong(key, default) as T
            }
            else -> {
                throw IllegalArgumentException("This type is not supported")
            }
        }
    }

    override fun <T> put(key: String, value: T) {
        when (value) {
            is Boolean -> {
                sharedPreferences.edit().putBoolean(key, value).apply()
            }
            is String -> {
                sharedPreferences.edit().putString(key, value).apply()
            }
            is Int -> {
                sharedPreferences.edit().putInt(key, value).apply()
            }
            is Float -> {
                sharedPreferences.edit().putFloat(key, value).apply()
            }
            is Long -> {
                sharedPreferences.edit().putLong(key, value).apply()
            }
            else -> {
                throw IllegalArgumentException("This type is not supported")
            }
        }
    }

    override fun remove(key: String) {
        sharedPreferences.edit().remove(key).apply()
    }

    override fun clear() {
        sharedPreferences.edit().clear().apply()
    }
}