package com.gojek.mqtt.client.username

internal class DefaultUsernameProviderImpl: UsernameProvider {
    override fun provideUsername(username: String): String {
        return username
    }
}
