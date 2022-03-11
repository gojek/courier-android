package com.gojek.mqtt.client.username

internal class CountrySectionProvider constructor(val countryCode: String): SectionProvider {
    override fun provideSection(): String {
        return countryCode
    }
}
