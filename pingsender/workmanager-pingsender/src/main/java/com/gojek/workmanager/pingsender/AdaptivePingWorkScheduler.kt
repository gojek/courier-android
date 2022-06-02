package com.gojek.workmanager.pingsender

import android.content.Context
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

internal class AdaptivePingWorkScheduler(workManager: WorkManager) : PingWorkScheduler(workManager) {
    override val workName: String
        get() = ADAPTIVE_MQTT_PING_SEND_WORKER
    override val workerClass: Class<out Worker>
        get() = AdaptivePingWorker::class.java
}

internal class AdaptivePingWorker(
    context: Context,
    private val workerParameters: WorkerParameters
) : Worker(context, workerParameters) {
    override fun doWork(): Result {
        val latch = CountDownLatch(1)
        WorkManagerPingSenderAdaptive.pingSender?.sendPing {
            latch.countDown()
        }
        val timeout = workerParameters.inputData.getLong(MQTT_PING_TIMEOUT_SECONDS, DEFAULT_PING_TIMEOUT_SECS)
        try {
            latch.await(timeout, TimeUnit.SECONDS)
        } catch (ignored: InterruptedException) {}
        return Result.success()
    }
}

internal const val ADAPTIVE_MQTT_PING_SEND_WORKER = "mqtt_adaptive_ping_sender_worker"
