package com.gojek.chuckmqtt.internal.domain

import android.annotation.SuppressLint
import com.gojek.chuckmqtt.internal.data.repository.MqttChuckRepository
import com.gojek.chuckmqtt.internal.domain.notification.NotificationUseCase
import com.gojek.chuckmqtt.internal.presentation.mapper.MqttTransactionUiModelMapper
import com.gojek.chuckmqtt.internal.presentation.model.MqttTransactionUiModel
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers

internal interface MqttChuckUseCase {
    fun initialise()
    fun getTransactionById(transactionId: Long): Observable<MqttTransactionUiModel>
    fun deleteOldTransactions(threshold: Long): Completable
    fun deleteAllTransactions(): Completable
    fun clearNotificationBuffer(): Completable
    fun observeAllMqttMessages(): Observable<List<MqttTransactionUiModel>>
}

internal class MqttChuckUseCaseImpl(
    private val notificationUseCase: NotificationUseCase,
    private val mqttChuckRepository: MqttChuckRepository,
    private val mqttTransactionUiModelMapper: MqttTransactionUiModelMapper
) : MqttChuckUseCase {

    override fun initialise() {
        observeRepository()
    }

    override fun getTransactionById(transactionId: Long): Observable<MqttTransactionUiModel> {
        return mqttChuckRepository.getTransactionById(transactionId)
            .map { transaction ->
                mqttTransactionUiModelMapper.map(transaction)
            }
    }

    override fun deleteOldTransactions(threshold: Long): Completable {
        return mqttChuckRepository.deleteOldTransaction(threshold)
    }

    override fun deleteAllTransactions(): Completable {
        return mqttChuckRepository.deleteAllTransactions()
    }

    override fun clearNotificationBuffer(): Completable {
        return Completable.fromCallable {
            notificationUseCase.clearBuffer()
        }
    }

    override fun observeAllMqttMessages(): Observable<List<MqttTransactionUiModel>> {
        return mqttChuckRepository.observeAllMqttMessages()
            .map { transactions ->
                transactions.map { transaction ->
                    mqttTransactionUiModelMapper.map(transaction)
                }.asReversed()
            }
    }

    @SuppressLint("CheckResult")
    private fun observeRepository() {
        mqttChuckRepository.observeNewMqttMessage()
            .subscribeOn(Schedulers.io())
            .doOnNext {
                notificationUseCase.showNotification(it)
            }
            .subscribe({}, {})
    }
}
