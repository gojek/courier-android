package com.gojek.timer.pingsender

import java.util.Timer

internal class TimerFactory {
    fun getTimer(name: String): Timer {
        return Timer(name)
    }
}
