package com.gojek.chuckmqtt.internal.presentation.transactiondetail.viewmodel

import androidx.lifecycle.ViewModel
import com.gojek.chuckmqtt.internal.presentation.base.viewmodel.MqttChuckMviViewModel
import com.gojek.chuckmqtt.internal.presentation.transactiondetail.actionprocessor.TransactionDetailActionProcessorProvider
import com.gojek.chuckmqtt.internal.presentation.transactiondetail.mvi.TransactionDetailAction
import com.gojek.chuckmqtt.internal.presentation.transactiondetail.mvi.TransactionDetailAction.GetTransactionDetailAction
import com.gojek.chuckmqtt.internal.presentation.transactiondetail.mvi.TransactionDetailAction.ShareTransactionDetailAction
import com.gojek.chuckmqtt.internal.presentation.transactiondetail.mvi.TransactionDetailIntent
import com.gojek.chuckmqtt.internal.presentation.transactiondetail.mvi.TransactionDetailIntent.GetTransactionDetailIntent
import com.gojek.chuckmqtt.internal.presentation.transactiondetail.mvi.TransactionDetailIntent.ShareTransactionDetailIntent
import com.gojek.chuckmqtt.internal.presentation.transactiondetail.mvi.TransactionDetailResult
import com.gojek.chuckmqtt.internal.presentation.transactiondetail.mvi.TransactionDetailResult.GetTransactionResult.GetTransactionLoadSuccess
import com.gojek.chuckmqtt.internal.presentation.transactiondetail.mvi.TransactionDetailResult.GetTransactionResult.GetTransactionLoading
import com.gojek.chuckmqtt.internal.presentation.transactiondetail.mvi.TransactionDetailViewEffect
import com.gojek.chuckmqtt.internal.presentation.transactiondetail.mvi.TransactionDetailViewEffect.ShareTransactionDetailViewEffect
import com.gojek.chuckmqtt.internal.presentation.transactiondetail.mvi.TransactionDetailViewState
import com.gojek.chuckmqtt.internal.presentation.transactiondetail.mvi.intentMerge
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.BiFunction
import io.reactivex.subjects.PublishSubject

internal class TransactionDetailViewModel(
    private val actionProcessorProvider: TransactionDetailActionProcessorProvider
) : ViewModel(),
    MqttChuckMviViewModel<TransactionDetailIntent, TransactionDetailViewState, TransactionDetailViewEffect> {

    private val _signal: PublishSubject<TransactionDetailIntent> = PublishSubject.create()
    private val signal = _signal.hide()

    private val _effect: PublishSubject<TransactionDetailViewEffect> = PublishSubject.create()
    private val effect = _effect.hide()

    private val states by lazy { compose() }

    private val reducer by lazy { reducer() }

    private val intentFilter: ObservableTransformer<TransactionDetailIntent, TransactionDetailIntent>
        get() = ObservableTransformer { intents ->
            intents.publish { shared ->
                intentMerge(shared)
            }
        }

    private fun compose() = signal.compose(intentFilter)
        .map(this::intentToAction)
        .compose(actionProcessorProvider.actionProcessor)
        .observeOn(AndroidSchedulers.mainThread())
        .doOnNext(::provisionEffect)
        .scan(TransactionDetailViewState.default(), reducer)
        .skip(1)
        .distinctUntilChanged()
        .share()

    private fun intentToAction(intent: TransactionDetailIntent): TransactionDetailAction {
        return when (intent) {
            is GetTransactionDetailIntent -> GetTransactionDetailAction(intent.transactionId)
            is ShareTransactionDetailIntent -> ShareTransactionDetailAction(intent.transactionId)
        }
    }

    private fun reducer() =
        BiFunction { prevState: TransactionDetailViewState, result: TransactionDetailResult ->
            when (result) {
                is GetTransactionLoadSuccess -> {
                    TransactionDetailViewState.reset(prevState)
                        .copy(transaction = result.transaction)
                }
                is GetTransactionLoading -> {
                    TransactionDetailViewState.reset(prevState)
                        .copy(showLoadingView = true)
                }
                is TransactionDetailResult.ShareTransactionDetailResult -> prevState
            }
        }

    private fun provisionEffect(result: TransactionDetailResult) {
        effectFromResult(result)?.let(_effect::onNext)
    }

    private fun effectFromResult(result: TransactionDetailResult): TransactionDetailViewEffect? {
        return when (result) {
            is TransactionDetailResult.ShareTransactionDetailResult -> {
                ShareTransactionDetailViewEffect(result.transaction)
            }
            else -> null
        }
    }

    override fun processIntents(intents: Observable<TransactionDetailIntent>) {
        intents.subscribe(_signal)
    }

    override fun states(): Observable<TransactionDetailViewState> {
        return states
    }

    override fun effects(): Observable<TransactionDetailViewEffect> {
        return effect
    }

    override fun onCleared() {
        _signal.onComplete()
        super.onCleared()
    }
}
