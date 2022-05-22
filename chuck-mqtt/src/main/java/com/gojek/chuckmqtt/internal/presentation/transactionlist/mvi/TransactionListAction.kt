package com.gojek.chuckmqtt.internal.presentation.transactionlist.mvi

import com.gojek.chuckmqtt.internal.presentation.base.viewmodel.MviAction

internal sealed class TransactionListAction :
    MviAction {

    object StartObservingAllTransactionsAction : TransactionListAction()

    data class OpenTransactionDetailAction(
        val transactionId: Long
    ) : TransactionListAction()

    object ClearTransactionHistoryAction : TransactionListAction()
}
