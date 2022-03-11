package com.gojek.chuckmqtt.internal.presentation.transactionlist.mvi

import com.gojek.chuckmqtt.internal.presentation.base.viewmodel.ViewState
import com.gojek.chuckmqtt.internal.presentation.model.MqttTransactionUiModel

internal data class TransactionListViewState(
    val showLoadingView: Boolean,
    val showEmptyView: Boolean,
    val transactionList: List<MqttTransactionUiModel>
) : ViewState {

    companion object {
        fun default() = TransactionListViewState(
            showLoadingView = false,
            showEmptyView = false,
            transactionList = listOf()
        )

        fun reset(prevState: TransactionListViewState) = prevState.copy(
            showLoadingView = false,
            showEmptyView = false,
            transactionList = listOf()
        )
    }
}