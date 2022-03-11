package com.gojek.chuckmqtt.internal.data.local.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.gojek.chuckmqtt.internal.data.local.entity.MqttTransaction
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single

@Dao
internal interface MqttTransactionDao {

    @Insert
    fun insert(mqttTransaction: MqttTransaction): Single<Long>

    @Query("DELETE from transactions")
    fun deleteAll(): Completable

    @Query("SELECT * FROM transactions")
    fun getAllMqttTransactions(): Observable<List<MqttTransaction>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    fun getMqttTransactionById(id: Long): Observable<MqttTransaction>

    @Query("DELETE FROM transactions WHERE transmission_time <= :threshold")
    fun deleteBefore(threshold: Long): Completable
}