package com.gojek.chuckmqtt.internal.presentation.transactiondetail.mvi

import com.gojek.chuckmqtt.internal.presentation.base.viewmodel.MviResult
import com.gojek.chuckmqtt.internal.presentation.model.MqttTransactionUiModel

internal sealed class TransactionDetailResult : MviResult {

    sealed class GetTransactionResult : TransactionDetailResult() {

        data class GetTransactionLoadSuccess(
            val transaction: MqttTransactionUiModel
        ) : GetTransactionResult()

        object GetTransactionLoading : GetTransactionResult()
    }

    data class ShareTransactionDetailResult(
        val transaction: MqttTransactionUiModel
    ) : TransactionDetailResult()
}