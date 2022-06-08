package com.gojek.chuckmqtt.internal.presentation.model

internal data class MqttTransactionUiModel(
    val id: Long,
    val packetName: String,
    val packetPreview: String,
    val isSent: Boolean,
    val transmissionTime: String,
    val bytesString: String,
    val packetInfo: String,
    val packetBody: String,
    val shareText: String
) {
    companion object {
        val EMPTY = MqttTransactionUiModel(
            id = 0,
            packetName = "",
            packetPreview = "",
            isSent = false,
            transmissionTime = "",
            bytesString = "",
            packetInfo = "",
            packetBody = "",
            shareText = ""
        )
    }
}
