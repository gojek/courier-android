package com.gojek.chuckmqtt.internal.presentation.transactiondetail.actionprocessor

import com.gojek.chuckmqtt.internal.domain.MqttChuckUseCase
import com.gojek.chuckmqtt.internal.presentation.transactiondetail.mvi.TransactionDetailAction
import com.gojek.chuckmqtt.internal.presentation.transactiondetail.mvi.TransactionDetailAction.*
import com.gojek.chuckmqtt.internal.presentation.transactiondetail.mvi.TransactionDetailResult
import com.gojek.chuckmqtt.internal.presentation.transactiondetail.mvi.TransactionDetailResult.*
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
                        shared.ofType(GetTransactionDetailAction::class.java)
                            .compose(getTransactionDetailActionProcessor),
                        shared.ofType(ShareTransactionDetailAction::class.java)
                            .compose(shareTransactionDetailActionProcessor)
                    )
                ).cast(TransactionDetailResult::class.java)
            }
        }

    private val getTransactionDetailActionProcessor =
        ObservableTransformer<GetTransactionDetailAction, GetTransactionResult> { actions ->
            actions.flatMap {
                mqttChuckUseCase.getTransactionById(it.transactionId)
                    .map<GetTransactionResult> { transactionUiModel ->
                        GetTransactionLoadSuccess(transactionUiModel)
                    }
                    .cast(GetTransactionResult::class.java)
                    .startWith(GetTransactionLoading)
                    .subscribeOn(Schedulers.computation())
            }
        }

    private val shareTransactionDetailActionProcessor =
        ObservableTransformer<ShareTransactionDetailAction, ShareTransactionDetailResult> { actions ->
            actions.flatMap {
                mqttChuckUseCase.getTransactionById(it.transactionId)
                    .map { transactionUiModel ->
                        ShareTransactionDetailResult(transactionUiModel)
                    }
                    .subscribeOn(Schedulers.computation())
            }
        }
}