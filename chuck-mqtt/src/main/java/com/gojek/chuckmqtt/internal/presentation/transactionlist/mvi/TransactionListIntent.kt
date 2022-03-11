package com.gojek.chuckmqtt.internal.presentation.transactionlist.mvi

import com.gojek.chuckmqtt.internal.presentation.base.viewmodel.MviIntent

internal sealed class TransactionListIntent:
    MviIntent {
    object StartObservingAllTransactionsIntent: TransactionListIntent()

    data class OpenTransactionDetailIntent(
        val transactionId: Long
    ) : TransactionListIntent()

    object ClearTransactionHistoryIntent : TransactionListIntent()
}