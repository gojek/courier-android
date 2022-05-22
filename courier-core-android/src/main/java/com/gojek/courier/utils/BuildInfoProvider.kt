package com.gojek.courier.utils

import android.os.Build
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY)
class BuildInfoProvider {
    val isKitkatOrHigher: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT

    val isMarshmallowOrHigher: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M

    val isLollipopOrHigher: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP

    val isOreoOrHigher: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
}
