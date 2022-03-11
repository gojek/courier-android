package com.gojek.keepalive.persistence

import com.gojek.keepalive.sharedpref.CourierSharedPreferences

internal interface KeepAlivePersistence {
    fun has(key: String): Boolean
    fun <T> get(key: String, default: T): T
    fun <T> put(key: String, value: T)
    fun remove(key: String)
}

internal class KeepAlivePersistenceImpl(
    private val sharedPreferences: CourierSharedPreferences
): KeepAlivePersistence {

    override fun has(key: String): Boolean {
        return sharedPreferences.has(key)
    }

    override fun <T> get(key: String, default: T): T {
        return sharedPreferences.get(key, default)
    }

    override fun <T> put(key: String, value: T) {
        sharedPreferences.put(key, value)
    }

    override fun remove(key: String) {
        sharedPreferences.remove(key)
    }
}