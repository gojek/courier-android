package com.gojek.appstatemanager

class AppStateManagerFactory private constructor() {
    companion object {
        fun create(): AppStateManager {
            return AppStateManagerImpl()
        }
    }
}
