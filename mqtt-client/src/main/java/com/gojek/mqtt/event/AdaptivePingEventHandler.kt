package com.gojek.mqtt.event

import com.gojek.mqtt.event.MqttEvent.BackgroundAlarmPingLimitReached
import com.gojek.mqtt.event.MqttEvent.MqttPingCancelledEvent
import com.gojek.mqtt.event.MqttEvent.MqttPingExceptionEvent
import com.gojek.mqtt.event.MqttEvent.MqttPingFailureEvent
import com.gojek.mqtt.event.MqttEvent.MqttPingInitiatedEvent
import com.gojek.mqtt.event.MqttEvent.MqttPingScheduledEvent
import com.gojek.mqtt.event.MqttEvent.MqttPingSuccessEvent
import com.gojek.mqtt.exception.toCourierException
import com.gojek.mqtt.pingsender.IPingSenderEvents

internal class AdaptivePingEventHandler(
    private val eventHandler: EventHandler
) : IPingSenderEvents {
    override fun exceptionInStart(e: Exception) {
        eventHandler.onEvent(
            MqttPingExceptionEvent(
                exception = e.toCourierException(),
                isAdaptive = true
            )
        )
    }

    override fun mqttPingScheduled(nextPingTimeSecs: Long, keepAliveSecs: Long) {
        eventHandler.onEvent(
            MqttPingScheduledEvent(
                nextPingTimeSecs = nextPingTimeSecs,
                keepAliveSecs = keepAliveSecs,
                isAdaptive = true
            )
        )
    }

    override fun mqttPingInitiated(serverUri: String, keepAliveSecs: Long) {
        eventHandler.onEvent(
            MqttPingInitiatedEvent(
                serverUri = serverUri,
                keepAliveSecs = keepAliveSecs,
                isAdaptive = true
            )
        )
    }

    override fun pingMqttTokenNull(serverUri: String, keepAliveSecs: Long) {
        eventHandler.onEvent(
            MqttPingCancelledEvent(
                serverUri = serverUri,
                keepAliveSecs = keepAliveSecs,
                isAdaptive = true
            )
        )
    }

    override fun pingEventSuccess(serverUri: String, timeTaken: Long, keepAliveSecs: Long) {
        eventHandler.onEvent(
            MqttPingSuccessEvent(
                serverUri = serverUri,
                timeTakenMillis = timeTaken,
                keepAliveSecs = keepAliveSecs,
                isAdaptive = true
            )
        )
    }

    override fun pingEventFailure(serverUri: String, timeTaken: Long, exception: Throwable, keepAliveSecs: Long) {
        eventHandler.onEvent(
            MqttPingFailureEvent(
                serverUri = serverUri,
                timeTakenMillis = timeTaken,
                keepAliveSecs = keepAliveSecs,
                exception = exception.toCourierException(),
                isAdaptive = true
            )
        )
    }

    override fun onBackgroundAlarmPingLimitReached() {
        eventHandler.onEvent(BackgroundAlarmPingLimitReached(isAdaptive = true))
    }
}
