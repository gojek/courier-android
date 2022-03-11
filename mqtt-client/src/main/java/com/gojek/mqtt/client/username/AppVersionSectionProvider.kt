package com.gojek.mqtt.client.username

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager


internal class AppVersionSectionProvider constructor(private val context: Context) : SectionProvider {
    override fun provideSection(): String {
        return try {
            val pInfo: PackageInfo =
                context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName
        } catch (e: Throwable) {
            ""
        }
    }
}
