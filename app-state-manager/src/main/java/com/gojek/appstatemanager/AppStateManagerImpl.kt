package com.gojek.appstatemanager

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.gojek.appstatemanager.AppState.FOREGROUND
import com.gojek.appstatemanager.AppState.BACKGROUND
import java.util.concurrent.CopyOnWriteArrayList

internal class AppStateManagerImpl : AppStateManager, LifecycleObserver {
    private var currentAppState: AppState = BACKGROUND
    private val listeners = CopyOnWriteArrayList<AppStateChangeListener>()

    init {
        Handler(Looper.getMainLooper()).post {
            ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onForeground() {
        dispatchAppState(FOREGROUND)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onBackground() {
        dispatchAppState(BACKGROUND)
    }

    private fun dispatchAppState(appState: AppState) {
        currentAppState = appState
        listeners.forEach {
            it.onAppStateChange(appState)
        }
    }

    override fun getCurrentAppState() = currentAppState

    override fun addAppStateListener(appStateChangeListener: AppStateChangeListener) {
        listeners.add(appStateChangeListener)
    }

    override fun removeAppStateListener(appStateChangeListener: AppStateChangeListener) {
        listeners.remove(appStateChangeListener)
    }
}
