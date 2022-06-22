package com.gojek.workmanager.pingsender

import androidx.work.WorkManager
import com.nhaarman.mockitokotlin2.mock
import kotlin.test.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class AdaptivePingWorkSchedulerTest {
    private val workManager = mock<WorkManager>()

    private val pingWorkScheduler = AdaptivePingWorkScheduler(workManager)

    @Test
    fun `assert workName`() {
        assertEquals(ADAPTIVE_MQTT_PING_SEND_WORKER, pingWorkScheduler.workName)
    }

    @Test
    fun `assert workerClass`() {
        assertEquals(AdaptivePingWorker::class.java, pingWorkScheduler.workerClass)
    }
}
