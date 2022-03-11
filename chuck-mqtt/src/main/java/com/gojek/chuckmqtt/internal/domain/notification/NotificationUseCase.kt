package com.gojek.chuckmqtt.internal.domain.notification

import android.util.LongSparseArray
import com.gojek.chuckmqtt.internal.NotificationHelper
import com.gojek.chuckmqtt.internal.domain.model.MqttTransactionDomainModel

internal interface NotificationUseCase {
    fun showNotification(mqttTransactionDomainModel: MqttTransactionDomainModel)
    fun clearBuffer()
    fun dismissNotification()
}

internal class NotificationUseCaseImpl(
    private val notificationHelper: NotificationHelper
) : NotificationUseCase {

    companion object {
        private const val BUFFER_SIZE = 10
        private val transactionBuffer = LongSparseArray<MqttTransactionDomainModel>()
        private val transactionIdsSet = HashSet<Long>()
    }

    override fun clearBuffer() {
        synchronized(transactionBuffer) {
            transactionBuffer.clear()
            transactionIdsSet.clear()
        }
    }

    override fun showNotification(mqttTransactionDomainModel: MqttTransactionDomainModel) {
        addToBuffer(mqttTransactionDomainModel)
        synchronized(transactionBuffer) {
            val notificationTextLines = mutableListOf<String>()
            (transactionBuffer.size() - 1 downTo 0).forEach { i ->
                notificationTextLines.add(transactionBuffer.valueAt(i).mqttWireMessage.packetName())
            }
            notificationHelper.showNotification(notificationTextLines, transactionIdsSet.size, BUFFER_SIZE)
        }
    }

    override fun dismissNotification() {
        notificationHelper.dismissNotification()
    }

    private fun addToBuffer(transaction: MqttTransactionDomainModel) {
        if (transaction.id == 0L) {
            // Don't store Transactions with an invalid ID (0).
            // Transaction with an Invalid ID will be shown twice in the notification
            // with both the invalid and the valid ID and we want to avoid this.
            return
        }
        synchronized(transactionBuffer) {
            transactionIdsSet.add(transaction.id)
            transactionBuffer.put(transaction.id, transaction)
            if (transactionBuffer.size() > BUFFER_SIZE) {
                transactionBuffer.removeAt(0)
            }
        }
    }

}