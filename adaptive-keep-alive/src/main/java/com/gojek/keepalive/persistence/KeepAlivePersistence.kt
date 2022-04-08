package com.gojek.keepalive.persistence

import com.gojek.keepalive.model.KeepAlivePersistenceModel
import com.gojek.keepalive.sharedpref.CourierSharedPreferences
import com.google.gson.Gson

internal interface KeepAlivePersistence {
    fun has(key: String): Boolean
    fun get(key: String): KeepAlivePersistenceModel
    fun put(key: String, value: KeepAlivePersistenceModel)
    fun remove(key: String)
}

internal class KeepAlivePersistenceImpl(
    private val sharedPreferences: CourierSharedPreferences,
    private val gson: Gson
): KeepAlivePersistence {

    override fun has(key: String): Boolean {
        return sharedPreferences.has(key)
    }

    override fun get(key: String): KeepAlivePersistenceModel {
        return gson.fromJson(sharedPreferences.get(key, ""), KeepAlivePersistenceModel::class.java)
    }

    override fun put(key: String, value: KeepAlivePersistenceModel) {
        sharedPreferences.put(key, gson.toJson(value))
    }

    override fun remove(key: String) {
        sharedPreferences.remove(key)
    }
}