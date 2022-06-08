package com.gojek.chuckmqtt.internal.presentation.transactiondetail.mvi

import com.gojek.chuckmqtt.internal.presentation.base.viewmodel.MviIntent

internal sealed class TransactionDetailIntent : MviIntent {
    data class GetTransactionDetailIntent(val transactionId: Long) : TransactionDetailIntent()
    data class ShareTransactionDetailIntent(val transactionId: Long) : TransactionDetailIntent()
}
