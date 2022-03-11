package com.gojek.chuckmqtt.internal.data.repository

import com.gojek.chuckmqtt.internal.MqttChuck
import com.gojek.chuckmqtt.internal.data.local.entity.MqttTransaction
import com.gojek.chuckmqtt.internal.data.network.Collector
import com.gojek.chuckmqtt.internal.data.network.model.CollectorModel
import com.gojek.chuckmqtt.internal.domain.mapper.MqttTransactionDomainModelMapper
import com.gojek.chuckmqtt.internal.domain.model.MqttTransactionDomainModel
import io.reactivex.Completable
import io.reactivex.Observable

internal interface MqttChuckRepository {
    fun getTransactionById(transactionId: Long): Observable<MqttTransactionDomainModel>
    fun observeNewMqttMessage(): Observable<MqttTransactionDomainModel>
    fun observeAllMqttMessages(): Observable<List<MqttTransactionDomainModel>>
    fun deleteOldTransaction(threshold: Long): Completable
    fun deleteAllTransactions(): Completable
}

internal class MqttChuckRepositoryImpl(
    private val collector: Collector,
    private val domainModelMapper: MqttTransactionDomainModelMapper
) : MqttChuckRepository {

    private val mqttTransactionDao by lazy { MqttChuck.getMqttDataBase().mqttTransactionDao() }

    private val retentionManager by lazy { MqttChuck.getRetentionManager() }

    override fun getTransactionById(transactionId: Long): Observable<MqttTransactionDomainModel> {
        return mqttTransactionDao.getMqttTransactionById(transactionId)
            .map { mqttTransaction ->
                domainModelMapper.map(mqttTransaction)
            }
    }

    override fun observeNewMqttMessage(): Observable<MqttTransactionDomainModel> {
        return collector.observe()
            .doOnNext {
                retentionManager.doMaintenance()
            }
            .map { collectorModel ->
                mapCollectorModelToMqttTransaction(collectorModel)
            }.concatMap { mqttTransaction ->
                mqttTransactionDao.insert(mqttTransaction)
                    .map { id ->
                        mqttTransaction.copy(
                            id = if(id == -1L) 0 else id
                        )
                    }
                    .toObservable()
            }
            .map { mqttTransaction ->
                domainModelMapper.map(mqttTransaction)
            }
    }

    override fun observeAllMqttMessages(): Observable<List<MqttTransactionDomainModel>> {
        return mqttTransactionDao.getAllMqttTransactions()
            .map { mqttTransactionList ->
                mqttTransactionList.map {
                    domainModelMapper.map(it)
                }
            }
    }

    override fun deleteOldTransaction(threshold: Long): Completable {
        return mqttTransactionDao.deleteBefore(threshold)
    }

    override fun deleteAllTransactions(): Completable {
        return mqttTransactionDao.deleteAll()
    }

    private fun mapCollectorModelToMqttTransaction(
        collectorModel: CollectorModel
    ): MqttTransaction {
        return with(collectorModel) {
            MqttTransaction(
                mqttWireMessageBytes = messageBytes,
                isPublished = isSent,
                requestDate = System.currentTimeMillis(),
                sizeInBytes = messageBytes.size.toLong()
            )
        }
    }
}