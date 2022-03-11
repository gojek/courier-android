package com.gojek.chuckmqtt.internal.utils.extensions

import android.view.View
import androidx.core.view.isGone
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.jakewharton.rxbinding3.view.clicks
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import java.util.concurrent.TimeUnit

internal fun View.show() {
    isVisible = true
}

internal fun View.hide() {
    isGone = true
}

internal fun View.invisible() {
    isInvisible = true
}

internal fun View.toggleVisibility() {
    isVisible = isVisible.not()
}

internal fun View.debouncedClicks(): Observable<Unit> = this.clicks()
    .debounce(200, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread())