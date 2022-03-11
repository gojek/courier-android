package com.gojek.networktracker.util

import android.os.Build

internal class BuildInfoProvider {
    fun isAndroidNAndAbove(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
    }
    fun isAndroidMAndAbove(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
    }
}