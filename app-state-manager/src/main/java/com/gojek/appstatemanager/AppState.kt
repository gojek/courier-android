package com.gojek.appstatemanager

sealed class AppState(val name: String) {
    object FOREGROUND : AppState("FG")
    object BACKGROUND : AppState("BG")
}
