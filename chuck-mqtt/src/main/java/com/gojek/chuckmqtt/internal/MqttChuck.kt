package com.gojek.chuckmqtt.internal

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import com.gojek.chuckmqtt.external.MqttChuckConfig
import com.gojek.chuckmqtt.internal.cleanup.RetentionManager
import com.gojek.chuckmqtt.internal.data.local.room.MqttChuckDatabase
import com.gojek.chuckmqtt.internal.data.network.Collector
import com.gojek.chuckmqtt.internal.data.repository.MqttChuckRepository
import com.gojek.chuckmqtt.internal.data.repository.MqttChuckRepositoryImpl
import com.gojek.chuckmqtt.internal.domain.MqttChuckUseCase
import com.gojek.chuckmqtt.internal.domain.MqttChuckUseCaseImpl
import com.gojek.chuckmqtt.internal.domain.mapper.MqttTransactionDomainModelMapper
import com.gojek.chuckmqtt.internal.domain.notification.NotificationUseCase
import com.gojek.chuckmqtt.internal.domain.notification.NotificationUseCaseImpl
import com.gojek.chuckmqtt.internal.presentation.mapper.MqttTransactionUiModelMapper
import com.gojek.chuckmqtt.internal.presentation.transactionlist.ui.activity.TransactionListActivity

@SuppressLint("StaticFieldLeak")
internal object MqttChuck {
    private lateinit var context: Context
    private lateinit var mqttChuckConfig: MqttChuckConfig
    private lateinit var collector: Collector
    private lateinit var mqttChuckRepository: MqttChuckRepository
    private lateinit var mqttChuckUseCase: MqttChuckUseCase
    private lateinit var notificationUseCase: NotificationUseCase
    private lateinit var retentionManager: RetentionManager

    internal fun initialise(context: Context, mqttChuckConfig: MqttChuckConfig) {
        this.context = context.applicationContext
        this.mqttChuckConfig = mqttChuckConfig
        this.collector = Collector()
        this.notificationUseCase = NotificationUseCaseImpl(NotificationHelper(context))
        this.mqttChuckUseCase = MqttChuckUseCaseImpl(getNotificationUseCase(context), getMqttChuckRepository(), MqttTransactionUiModelMapper())
        this.retentionManager = RetentionManager(context, mqttChuckConfig.retentionPeriod)

        mqttChuckUseCase.initialise()
    }

    internal fun collector() = collector

    internal fun mqttChuckUseCase() = mqttChuckUseCase

    internal fun notificationUseCase() = notificationUseCase

    internal fun getMqttDataBase() = MqttChuckDatabase.create(context)

    internal fun getRetentionManager() = retentionManager

    @JvmStatic
    fun getLaunchIntent(context: Context): Intent {
        return Intent(context, TransactionListActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    private fun getMqttChuckRepository(): MqttChuckRepository {
        return MqttChuckRepositoryImpl(collector(), MqttTransactionDomainModelMapper())
    }

    private fun getNotificationUseCase(context: Context): NotificationUseCase {
        return NotificationUseCaseImpl(
            NotificationHelper(
                context
            )
        )
    }
}
