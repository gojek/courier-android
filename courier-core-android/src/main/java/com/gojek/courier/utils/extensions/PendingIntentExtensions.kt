package com.gojek.courier.utils.extensions

import android.app.PendingIntent.FLAG_IMMUTABLE
import android.os.Build
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY)
fun Int.addImmutableFlag(): Int {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        this.plus(FLAG_IMMUTABLE)
    } else {
        this
    }
}