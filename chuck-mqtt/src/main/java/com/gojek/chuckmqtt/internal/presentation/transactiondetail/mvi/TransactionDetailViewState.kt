package com.gojek.chuckmqtt.internal.presentation.transactiondetail.mvi

import com.gojek.chuckmqtt.internal.presentation.base.viewmodel.ViewState
import com.gojek.chuckmqtt.internal.presentation.model.MqttTransactionUiModel

internal data class TransactionDetailViewState(
    val showLoadingView: Boolean,
    val showEmptyView: Boolean,
    val transaction: MqttTransactionUiModel
) : ViewState {

    companion object {
        fun default() = TransactionDetailViewState(
            showLoadingView = false,
            showEmptyView = false,
            transaction = MqttTransactionUiModel.EMPTY
        )

        fun reset(prevState: TransactionDetailViewState) = prevState.copy(
            showLoadingView = false,
            showEmptyView = false,
            transaction = MqttTransactionUiModel.EMPTY
        )
    }
}