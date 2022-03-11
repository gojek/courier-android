package com.gojek.chuckmqtt.internal.presentation.transactiondetail.mvi

import com.gojek.chuckmqtt.internal.presentation.base.viewmodel.MviAction

internal sealed class TransactionDetailAction : MviAction {

    data class GetTransactionDetailAction(val transactionId: Long) : TransactionDetailAction()
    data class ShareTransactionDetailAction(val transactionId: Long) : TransactionDetailAction()
}