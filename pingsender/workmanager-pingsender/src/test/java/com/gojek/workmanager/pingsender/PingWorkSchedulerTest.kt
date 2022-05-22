package com.gojek.workmanager.pingsender

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import kotlin.test.assertEquals

@RunWith(MockitoJUnitRunner::class)
class PingWorkSchedulerTest {
    private val workManager = mock<WorkManager>()

    private val pingWorkScheduler = TestPingWorkScheduler(workManager)

    @Test
    fun `test scheduleWork`() {
        val delayInMillis = 10000L
        val timeout = 10L
        pingWorkScheduler.schedulePingWork(delayInMillis, timeout)

        val argumentCaptor1 = argumentCaptor<String>()
        val argumentCaptor2 = argumentCaptor<ExistingWorkPolicy>()
        val argumentCaptor3 = argumentCaptor<OneTimeWorkRequest>()
        verify(workManager).enqueueUniqueWork(argumentCaptor1.capture(), argumentCaptor2.capture(), argumentCaptor3.capture())
        assertEquals(pingWorkScheduler.workName, argumentCaptor1.lastValue)
        assertEquals(ExistingWorkPolicy.REPLACE, argumentCaptor2.lastValue)
        assertEquals(pingWorkScheduler.workerClass.name, argumentCaptor3.lastValue.workSpec.workerClassName)
        assertEquals(delayInMillis, argumentCaptor3.lastValue.workSpec.initialDelay)
        assertEquals(timeout, argumentCaptor3.lastValue.workSpec.input.getLong(MQTT_PING_TIMEOUT_SECONDS, 0))
    }

    @Test
    fun `test cancelWork`() {
        pingWorkScheduler.cancelWork()

        verify(workManager).cancelUniqueWork(pingWorkScheduler.workName)
    }

    @After
    fun teardown() {
        verifyNoMoreInteractions(workManager)
    }
}

internal class TestPingWorkScheduler(workManager: WorkManager) : PingWorkScheduler(workManager) {
    override val workName: String
        get() = "test-work"
    override val workerClass: Class<out Worker>
        get() = TestWorker::class.java
}

internal class TestWorker(
    context: Context,
    workerParameters: WorkerParameters
) : Worker(context, workerParameters) {
    override fun doWork(): Result {
        return Result.success()
    }
}
