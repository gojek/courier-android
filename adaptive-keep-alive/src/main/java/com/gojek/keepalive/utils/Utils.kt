package com.gojek.keepalive.utils

import android.os.Build

internal class Utils {

    val isKitkatOrHigher: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT

    val isMarshmallowOrHigher: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
}