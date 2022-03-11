package com.gojek.mqtt.client.username

internal interface UsernameProvider {
    fun provideUsername(username: String): String
}
