package com.gojek.chuckmqtt.internal.presentation.transactionlist.viewmodel

import androidx.lifecycle.ViewModel
import com.gojek.chuckmqtt.internal.presentation.base.viewmodel.MqttChuckMviViewModel
import com.gojek.chuckmqtt.internal.presentation.transactionlist.actionprocessor.TransactionListActionProcessorProvider
import com.gojek.chuckmqtt.internal.presentation.transactionlist.mvi.TransactionListAction
import com.gojek.chuckmqtt.internal.presentation.transactionlist.mvi.TransactionListAction.ClearTransactionHistoryAction
import com.gojek.chuckmqtt.internal.presentation.transactionlist.mvi.TransactionListAction.OpenTransactionDetailAction
import com.gojek.chuckmqtt.internal.presentation.transactionlist.mvi.TransactionListAction.StartObservingAllTransactionsAction
import com.gojek.chuckmqtt.internal.presentation.transactionlist.mvi.TransactionListIntent
import com.gojek.chuckmqtt.internal.presentation.transactionlist.mvi.TransactionListIntent.ClearTransactionHistoryIntent
import com.gojek.chuckmqtt.internal.presentation.transactionlist.mvi.TransactionListIntent.OpenTransactionDetailIntent
import com.gojek.chuckmqtt.internal.presentation.transactionlist.mvi.TransactionListIntent.StartObservingAllTransactionsIntent
import com.gojek.chuckmqtt.internal.presentation.transactionlist.mvi.TransactionListResult
import com.gojek.chuckmqtt.internal.presentation.transactionlist.mvi.TransactionListResult.AllTransactionsResult.AllTransactionsLoading
import com.gojek.chuckmqtt.internal.presentation.transactionlist.mvi.TransactionListResult.AllTransactionsResult.AllTransactionsSuccess
import com.gojek.chuckmqtt.internal.presentation.transactionlist.mvi.TransactionListResult.ClearTransactionHistoryResult
import com.gojek.chuckmqtt.internal.presentation.transactionlist.mvi.TransactionListResult.OpenTransactionDetailResult
import com.gojek.chuckmqtt.internal.presentation.transactionlist.mvi.TransactionListViewEffect
import com.gojek.chuckmqtt.internal.presentation.transactionlist.mvi.TransactionListViewEffect.OpenTransactionDetailViewEffect
import com.gojek.chuckmqtt.internal.presentation.transactionlist.mvi.TransactionListViewState
import com.gojek.chuckmqtt.internal.presentation.transactionlist.mvi.intentMerge
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.BiFunction
import io.reactivex.subjects.PublishSubject

internal class TransactionListViewModel(
    private val actionProcessorProvider: TransactionListActionProcessorProvider
) : ViewModel(),
    MqttChuckMviViewModel<TransactionListIntent, TransactionListViewState, TransactionListViewEffect> {

    private val _signal: PublishSubject<TransactionListIntent> = PublishSubject.create()
    private val signal = _signal.cast(TransactionListIntent::class.java)

    private val _effect: PublishSubject<TransactionListViewEffect> = PublishSubject.create()
    private val effect = _effect.cast(TransactionListViewEffect::class.java)

    private val states by lazy { compose() }

    private val reducer by lazy { reducer() }

    private val intentFilter: ObservableTransformer<TransactionListIntent, TransactionListIntent>
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
        .scan(TransactionListViewState.default(), reducer)
        .skip(1)
        .distinctUntilChanged()
        .share()

    private fun intentToAction(intent: TransactionListIntent): TransactionListAction {
        return when (intent) {
            is StartObservingAllTransactionsIntent -> StartObservingAllTransactionsAction
            is OpenTransactionDetailIntent -> OpenTransactionDetailAction(intent.transactionId)
            is ClearTransactionHistoryIntent -> ClearTransactionHistoryAction
        }
    }

    private fun reducer() =
        BiFunction { prevState: TransactionListViewState, result: TransactionListResult ->
            when (result) {
                is AllTransactionsSuccess -> {
                    TransactionListViewState.reset(prevState)
                        .copy(transactionList = result.transactionList)
                }
                is AllTransactionsLoading -> {
                    TransactionListViewState.reset(prevState)
                        .copy(showLoadingView = true)
                }
                is OpenTransactionDetailResult -> prevState
                is ClearTransactionHistoryResult -> prevState
            }
        }

    private fun provisionEffect(result: TransactionListResult) {
        effectFromResult(result)?.let(_effect::onNext)
    }

    private fun effectFromResult(result: TransactionListResult): TransactionListViewEffect? {
        return when (result) {
            is OpenTransactionDetailResult -> OpenTransactionDetailViewEffect(result.transactionId)
            else -> null
        }
    }

    override fun processIntents(intents: Observable<TransactionListIntent>) {
        intents.subscribe(_signal)
    }

    fun dispatchIntent(intent: TransactionListIntent) {
        _signal.onNext(intent)
    }

    override fun states(): Observable<TransactionListViewState> {
        return states
    }

    override fun effects(): Observable<TransactionListViewEffect> {
        return effect
    }

    override fun onCleared() {
        _signal.onComplete()
        super.onCleared()
    }
}
