package com.gojek.chuckmqtt.internal.presentation.transactiondetail.mvi

import com.gojek.chuckmqtt.internal.presentation.base.viewmodel.ViewEffect
import com.gojek.chuckmqtt.internal.presentation.model.MqttTransactionUiModel

internal sealed class TransactionDetailViewEffect : ViewEffect {
    data class ShareTransactionDetailViewEffect(
        val mqttTransactionUiModel: MqttTransactionUiModel
    ) : TransactionDetailViewEffect()
}
