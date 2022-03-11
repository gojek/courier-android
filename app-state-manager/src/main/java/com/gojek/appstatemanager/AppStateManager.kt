package com.gojek.appstatemanager

interface AppStateChangeListener {
    fun onAppStateChange(appState: AppState)
}

interface AppStateManager {
    fun getCurrentAppState(): AppState

    fun addAppStateListener(appStateChangeListener: AppStateChangeListener)
    fun removeAppStateListener(appStateChangeListener: AppStateChangeListener)
}
