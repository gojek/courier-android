package com.gojek.mqtt.client.username

import android.content.Context
import com.gojek.appstatemanager.AppStateManager
import com.gojek.mqtt.model.MqttConnectOptions
import com.gojek.networktracker.NetworkStateTracker
import java.lang.StringBuilder

internal class UsernameProviderImpl(
    val context: Context,
    val networkStateTracker: NetworkStateTracker,
    val appStateManager: AppStateManager,
    val countryCode: String
) : UsernameProvider {

    private val usernameSectionProviderList: List<SectionProvider> = listOf(
        CountrySectionProvider(countryCode),
        NetworkSectionProvider(networkStateTracker),
        PlatformSectionProvider(),
        AppVersionSectionProvider(context),
        AppStateSectionProvider(appStateManager)
    )

    override fun provideUsername(username: String): String {
        val sb = StringBuilder("$username:")
        for (usernameSection in usernameSectionProviderList) {
            sb.append("${usernameSection.provideSection()}:")
        }
        return sb.toString().removeSuffix(":")
    }
}
