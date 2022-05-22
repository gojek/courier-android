package com.gojek.workmanager.pingsender

import androidx.work.WorkManager
import com.nhaarman.mockitokotlin2.mock
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import kotlin.test.assertEquals

@RunWith(MockitoJUnitRunner::class)
class NonAdaptivePingWorkSchedulerTest {
    private val workManager = mock<WorkManager>()

    private val pingWorkScheduler = NonAdaptivePingWorkScheduler(workManager)

    @Test
    fun `assert workName`() {
        assertEquals(MQTT_PING_SEND_WORKER, pingWorkScheduler.workName)
    }

    @Test
    fun `assert workerClass`() {
        assertEquals(NonAdaptivePingWorker::class.java, pingWorkScheduler.workerClass)
    }
}
