package com.gojek.mqtt.client.username

import com.gojek.appstatemanager.AppStateManager

internal class AppStateSectionProvider(private val appStateManager: AppStateManager) : SectionProvider {
    override fun provideSection(): String {
        return appStateManager.getCurrentAppState().name
    }
}
