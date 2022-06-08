package com.gojek.chuckmqtt.internal.presentation.base.viewmodel

import io.reactivex.Observable

internal interface MviResult
internal interface MviAction
internal interface MviIntent
internal interface ViewState
internal interface ViewEffect

internal interface MqttChuckMviViewModel<I : MviIntent, S : ViewState, E : ViewEffect> {
    fun processIntents(intents: Observable<I>)
    fun states(): Observable<S>
    fun effects(): Observable<E>
}

internal interface MviView<I : MviIntent, in S : ViewState> {
    fun intents(): Observable<I>
    fun render(state: S)
}
