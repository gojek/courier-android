package com.gojek.chuckmqtt.internal.presentation.transactionlist.mvi

import com.gojek.chuckmqtt.internal.presentation.base.viewmodel.ViewEffect

internal sealed class TransactionListViewEffect : ViewEffect {
    data class OpenTransactionDetailViewEffect(
        val transactionId: Long
    ) : TransactionListViewEffect()
}