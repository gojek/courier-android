package com.gojek.chuckmqtt.internal.presentation.base.activity

import androidx.appcompat.app.AppCompatActivity

internal abstract class BaseChuckMqttActivity: AppCompatActivity() {

    override fun onResume() {
        super.onResume()
        isInForeground = true
    }

    override fun onPause() {
        super.onPause()
        isInForeground = false
    }

    companion object {
        @Volatile
        var isInForeground: Boolean = false
            private set
    }
}