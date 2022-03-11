package com.gojek.mqtt.client.username

internal interface SectionProvider {
    fun provideSection(): String
}
