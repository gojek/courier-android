package com.gojek.workmanager.pingsender

import android.content.Context
import androidx.work.WorkManager
import com.gojek.mqtt.pingsender.AdaptiveMqttPingSender
import com.gojek.mqtt.pingsender.MqttPingSender

class WorkPingSenderFactory private constructor() {
    companion object {
        fun createMqttPingSender(context: Context, pingSenderConfig: WorkManagerPingSenderConfig): MqttPingSender {
            val workManager = WorkManager.getInstance(context.applicationContext)
            return WorkManagerPingSender(
                pingWorkScheduler = NonAdaptivePingWorkScheduler(workManager),
                pingSenderConfig = pingSenderConfig
            )
        }

        fun createAdaptiveMqttPingSender(context: Context, pingSenderConfig: WorkManagerPingSenderConfig): AdaptiveMqttPingSender {
            val workManager = WorkManager.getInstance(context.applicationContext)
            return WorkManagerPingSenderAdaptive(
                pingWorkScheduler = AdaptivePingWorkScheduler(workManager),
                pingSenderConfig = pingSenderConfig
            )
        }
    }
}
