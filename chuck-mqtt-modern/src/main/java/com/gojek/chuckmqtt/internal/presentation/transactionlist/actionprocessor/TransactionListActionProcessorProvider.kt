package com.gojek.chuckmqtt.internal.presentation.transactionlist.actionprocessor

import com.gojek.chuckmqtt.internal.domain.MqttChuckUseCase
import com.gojek.chuckmqtt.internal.presentation.transactionlist.mvi.TransactionListAction
import com.gojek.chuckmqtt.internal.presentation.transactionlist.mvi.TransactionListAction.ClearTransactionHistoryAction
import com.gojek.chuckmqtt.internal.presentation.transactionlist.mvi.TransactionListAction.OpenTransactionDetailAction
import com.gojek.chuckmqtt.internal.presentation.transactionlist.mvi.TransactionListAction.StartObservingAllTransactionsAction
import com.gojek.chuckmqtt.internal.presentation.transactionlist.mvi.TransactionListResult
import com.gojek.chuckmqtt.internal.presentation.transactionlist.mvi.TransactionListResult.AllTransactionsResult
import com.gojek.chuckmqtt.internal.presentation.transactionlist.mvi.TransactionListResult.AllTransactionsResult.AllTransactionsLoading
import com.gojek.chuckmqtt.internal.presentation.transactionlist.mvi.TransactionListResult.AllTransactionsResult.AllTransactionsSuccess
import com.gojek.chuckmqtt.internal.presentation.transactionlist.mvi.TransactionListResult.ClearTransactionHistoryResult
import com.gojek.chuckmqtt.internal.presentation.transactionlist.mvi.TransactionListResult.OpenTransactionDetailResult
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.schedulers.Schedulers

internal interface TransactionListActionProcessorProvider {
    val actionProcessor: ObservableTransformer<TransactionListAction, TransactionListResult>
}

internal class DefaultTransactionListActionProcessorProvider(
    private val mqttChuckUseCase: MqttChuckUseCase
) : TransactionListActionProcessorProvider {
    override val actionProcessor: ObservableTransformer<TransactionListAction, TransactionListResult>
        get() = ObservableTransformer { actions ->
            actions.publish { shared ->
                Observable.merge(
                    listOf(
                        shared.ofType(StartObservingAllTransactionsAction::class.java)
                            .compose(startObservingAllTransactionsProcessor),
                        shared.ofType(OpenTransactionDetailAction::class.java)
                            .compose(openTransactionDetailActionProcessor),
                        shared.ofType(ClearTransactionHistoryAction::class.java)
                            .compose(clearTransactionHistoryActionProcessor)
                    )
                )
            }
        }

    private val startObservingAllTransactionsProcessor =
        ObservableTransformer<StartObservingAllTransactionsAction, AllTransactionsResult> { actions ->
            actions.flatMap {
                mqttChuckUseCase.observeAllMqttMessages()
                    .map<AllTransactionsSuccess> {
                        AllTransactionsSuccess(it)
                    }
                    .cast(AllTransactionsResult::class.java)
                    .startWith(AllTransactionsLoading)
                    .subscribeOn(Schedulers.computation())
            }
        }

    private val openTransactionDetailActionProcessor =
        ObservableTransformer<OpenTransactionDetailAction, OpenTransactionDetailResult> { actions ->
            actions.flatMap {
                Observable.just(OpenTransactionDetailResult(it.transactionId))
            }
        }

    private val clearTransactionHistoryActionProcessor =
        ObservableTransformer<ClearTransactionHistoryAction, ClearTransactionHistoryResult> { actions ->
            actions.flatMap {
                mqttChuckUseCase.deleteAllTransactions()
                    .andThen(mqttChuckUseCase.clearNotificationBuffer())
                    .toObservable<Unit>()
                    .map { ClearTransactionHistoryResult }
                    .subscribeOn(Schedulers.computation())
            }
        }
}
