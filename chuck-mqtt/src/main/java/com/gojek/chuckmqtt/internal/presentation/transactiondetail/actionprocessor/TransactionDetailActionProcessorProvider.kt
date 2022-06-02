package com.gojek.chuckmqtt.internal.presentation.transactiondetail.actionprocessor

import com.gojek.chuckmqtt.internal.domain.MqttChuckUseCase
import com.gojek.chuckmqtt.internal.presentation.transactiondetail.mvi.TransactionDetailAction
import com.gojek.chuckmqtt.internal.presentation.transactiondetail.mvi.TransactionDetailResult
import com.gojek.chuckmqtt.internal.presentation.transactiondetail.mvi.TransactionDetailResult.GetTransactionResult.GetTransactionLoadSuccess
import com.gojek.chuckmqtt.internal.presentation.transactiondetail.mvi.TransactionDetailResult.GetTransactionResult.GetTransactionLoading
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.schedulers.Schedulers

internal interface TransactionDetailActionProcessorProvider {
    val actionProcessor: ObservableTransformer<TransactionDetailAction, TransactionDetailResult>
}

internal class DefaultTransactionDetailActionProcessorProvider(
    private val mqttChuckUseCase: MqttChuckUseCase
) : TransactionDetailActionProcessorProvider {
    override val actionProcessor: ObservableTransformer<TransactionDetailAction, TransactionDetailResult>
        get() = ObservableTransformer { actions ->
            actions.publish { shared ->
                Observable.merge(
                    listOf(
                        shared.ofType(TransactionDetailAction.GetTransactionDetailAction::class.java)
                            .compose(getTransactionDetailActionProcessor),
                        shared.ofType(TransactionDetailAction.ShareTransactionDetailAction::class.java)
                            .compose(shareTransactionDetailActionProcessor)
                    )
                ).cast(TransactionDetailResult::class.java)
            }
        }

    private val getTransactionDetailActionProcessor =
        ObservableTransformer<TransactionDetailAction.GetTransactionDetailAction, TransactionDetailResult.GetTransactionResult> { actions ->
            actions.flatMap {
                mqttChuckUseCase.getTransactionById(it.transactionId)
                    .map<TransactionDetailResult.GetTransactionResult> { transactionUiModel ->
                        GetTransactionLoadSuccess(transactionUiModel)
                    }
                    .cast(TransactionDetailResult.GetTransactionResult::class.java)
                    .startWith(GetTransactionLoading)
                    .subscribeOn(Schedulers.computation())
            }
        }

    private val shareTransactionDetailActionProcessor =
        ObservableTransformer<TransactionDetailAction.ShareTransactionDetailAction, TransactionDetailResult.ShareTransactionDetailResult> { actions ->
            actions.flatMap {
                mqttChuckUseCase.getTransactionById(it.transactionId)
                    .map { transactionUiModel ->
                        TransactionDetailResult.ShareTransactionDetailResult(transactionUiModel)
                    }
                    .subscribeOn(Schedulers.computation())
            }
        }
}
