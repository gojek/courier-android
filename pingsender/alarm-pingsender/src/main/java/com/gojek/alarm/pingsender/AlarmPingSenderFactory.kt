package com.gojek.alarm.pingsender

import android.content.Context
import com.gojek.mqtt.pingsender.AdaptiveMqttPingSender
import com.gojek.mqtt.pingsender.MqttPingSender

class AlarmPingSenderFactory private constructor() {
    companion object {
        fun createMqttPingSender(context: Context, pingSenderConfig: AlarmPingSenderConfig): MqttPingSender {
            return AlarmPingSender(context.applicationContext, pingSenderConfig)
        }

        fun createAdaptiveMqttPingSender(context: Context, pingSenderConfig: AlarmPingSenderConfig): AdaptiveMqttPingSender {
            return AdaptiveAlarmPingSender(context.applicationContext, pingSenderConfig)
        }
    }
}
