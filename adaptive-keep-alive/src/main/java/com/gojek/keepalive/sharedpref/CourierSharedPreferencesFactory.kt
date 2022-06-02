package com.gojek.keepalive.sharedpref

import android.content.Context
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class CourierSharedPreferencesFactory private constructor() {
    companion object {
        fun create(context: Context, prefName: String): CourierSharedPreferences {
            return CourierSharedPreferencesImpl(
                context.getSharedPreferences(
                    prefName,
                    Context.MODE_PRIVATE
                )
            )
        }
    }
}
