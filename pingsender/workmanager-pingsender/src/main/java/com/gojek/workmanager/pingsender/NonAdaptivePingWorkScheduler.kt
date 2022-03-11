package com.gojek.workmanager.pingsender

import android.content.Context
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

internal class NonAdaptivePingWorkScheduler(workManager: WorkManager) : PingWorkScheduler(workManager) {
    override val workName: String
        get() = MQTT_PING_SEND_WORKER
    override val workerClass: Class<out Worker>
        get() = NonAdaptivePingWorker::class.java
}

internal class NonAdaptivePingWorker(
    context: Context,
    private val workerParameters: WorkerParameters
) : Worker(context, workerParameters) {
    override fun doWork(): Result {
        val latch = CountDownLatch(1)
        WorkManagerPingSender.pingSender?.sendPing {
            latch.countDown()
        }
        val timeout = workerParameters.inputData.getLong(MQTT_PING_TIMEOUT_SECONDS, DEFAULT_PING_TIMEOUT_SECS)
        try {
            latch.await(timeout, TimeUnit.SECONDS)
        } catch (ignored: InterruptedException) {}
        return Result.success()
    }
}

internal const val MQTT_PING_SEND_WORKER = "mqtt_ping_sender_worker"