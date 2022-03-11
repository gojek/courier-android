package com.gojek.workmanager.pingsender

import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.Worker
import java.util.concurrent.TimeUnit

internal abstract class PingWorkScheduler(private val workManager: WorkManager) {
    fun schedulePingWork(delayInMillis: Long, workTimeout: Long) {
        val request = OneTimeWorkRequest.Builder(workerClass)
            .setInitialDelay(delayInMillis, TimeUnit.MILLISECONDS)
            .setInputData(Data.Builder().putLong(MQTT_PING_TIMEOUT_SECONDS, workTimeout).build())
            .build()
        workManager.enqueueUniqueWork(
            workName,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancelWork() {
        workManager.cancelUniqueWork(
            workName
        )
    }
    
    abstract val workName: String
    abstract val workerClass: Class<out Worker>
}

internal const val MQTT_PING_TIMEOUT_SECONDS = "ping_timeout_seconds"