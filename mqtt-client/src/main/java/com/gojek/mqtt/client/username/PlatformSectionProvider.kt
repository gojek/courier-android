package com.gojek.mqtt.client.username

internal class PlatformSectionProvider : SectionProvider {
    override fun provideSection(): String {
        return "ANDROID"
    }
}
