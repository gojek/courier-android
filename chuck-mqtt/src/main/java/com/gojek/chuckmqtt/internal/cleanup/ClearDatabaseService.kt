package com.gojek.chuckmqtt.internal.cleanup

import android.annotation.SuppressLint
import android.app.IntentService
import android.content.Intent
import com.gojek.chuckmqtt.internal.MqttChuck
import io.reactivex.schedulers.Schedulers
import java.io.Serializable

internal class ClearDatabaseService : IntentService(CLEAN_DATABASE_SERVICE_NAME) {

    private val mqttChuckUseCase by lazy { MqttChuck.mqttChuckUseCase() }

    private val notificationUseCase by lazy { MqttChuck.notificationUseCase() }

    @SuppressLint("CheckResult")
    override fun onHandleIntent(intent: Intent?) {
        when (intent?.getSerializableExtra(EXTRA_ITEM_TO_CLEAR)) {
            is ClearAction.Transaction -> {
                mqttChuckUseCase.deleteAllTransactions()
                    .andThen(mqttChuckUseCase.clearNotificationBuffer())
                    .subscribeOn(Schedulers.computation())
                    .subscribe(
                        {
                            notificationUseCase.dismissNotification()
                        }, {
                        // no ops
                    }
                    )
            }
        }
    }

    sealed class ClearAction : Serializable {
        object Transaction : ClearAction()
    }

    companion object {
        const val CLEAN_DATABASE_SERVICE_NAME = "MQTT-Chuck-ClearDatabaseService"
        const val EXTRA_ITEM_TO_CLEAR = "EXTRA_ITEM_TO_CLEAR"
    }
}
