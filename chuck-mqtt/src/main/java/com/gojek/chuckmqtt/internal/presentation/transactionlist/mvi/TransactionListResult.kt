package com.gojek.chuckmqtt.internal.presentation.transactionlist.mvi

import com.gojek.chuckmqtt.internal.presentation.base.viewmodel.MviResult
import com.gojek.chuckmqtt.internal.presentation.model.MqttTransactionUiModel

internal sealed class TransactionListResult :
    MviResult {

    sealed class AllTransactionsResult : TransactionListResult() {

        data class AllTransactionsSuccess(
            val transactionList: List<MqttTransactionUiModel>
        ) : AllTransactionsResult()

        object AllTransactionsLoading : AllTransactionsResult()
    }

    data class OpenTransactionDetailResult(
        val transactionId: Long
    ) : TransactionListResult()

    object ClearTransactionHistoryResult: TransactionListResult()
}