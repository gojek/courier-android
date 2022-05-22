package com.gojek.keepalive.sharedpref

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal interface CourierSharedPreferences {
    fun has(key: String): Boolean
    fun <T> get(key: String, default: T): T
    fun <T> put(key: String, value: T)
    fun remove(key: String)
    fun clear()
}
