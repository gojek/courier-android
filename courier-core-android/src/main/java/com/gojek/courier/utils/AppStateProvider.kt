package com.gojek.courier.utils

import androidx.annotation.RestrictTo
import androidx.lifecycle.Lifecycle.State.STARTED
import androidx.lifecycle.ProcessLifecycleOwner

@RestrictTo(RestrictTo.Scope.LIBRARY)
class AppStateProvider {
    val isAppInForeground: Boolean
        get() = ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(STARTED)
}