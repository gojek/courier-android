package com.gojek.mqtt.scheduler

import android.os.Handler
import android.os.HandlerThread
import com.gojek.courier.QoS
import com.gojek.courier.logging.ILogger
import com.gojek.mqtt.client.IClientSchedulerBridge
import com.gojek.mqtt.constants.MQTT_WAIT_BEFORE_RECONNECT_TIME_MS
import com.gojek.mqtt.event.EventHandler
import com.gojek.mqtt.event.MqttEvent.HandlerThreadNotAliveEvent
import com.gojek.mqtt.policies.connectretrytime.ConnectRetryTimeConfig
import com.gojek.mqtt.scheduler.runnable.ActivityCheckRunnable
import com.gojek.mqtt.scheduler.runnable.AuthFailureRunnable
import com.gojek.mqtt.scheduler.runnable.ConnectionCheckRunnable
import com.gojek.mqtt.scheduler.runnable.DisconnectRunnable
import com.gojek.mqtt.scheduler.runnable.MqttExceptionRunnable
import com.gojek.mqtt.scheduler.runnable.ResetParamsRunnable
import com.gojek.mqtt.scheduler.runnable.SubscribeRunnable
import com.gojek.mqtt.scheduler.runnable.UnsubscribeRunnable

internal class MqttRunnableScheduler(
    private val handlerThread: HandlerThread,
    private val mqttThreadHandler: Handler,
    private val clientSchedulerBridge: IClientSchedulerBridge,
    private val logger: ILogger,
    private val eventHandler: EventHandler,
    private val activityCheckIntervalSeconds: Int
) : IRunnableScheduler {
    private val connectionCheckRunnable = ConnectionCheckRunnable(clientSchedulerBridge)
    private val mqttExceptionRunnable = MqttExceptionRunnable(clientSchedulerBridge)
    private val disconnectRunnable = DisconnectRunnable(
        clientSchedulerBridge
    )
    private val activityCheckRunnable = ActivityCheckRunnable(clientSchedulerBridge, logger)
    private val resetParamsRunnable = ResetParamsRunnable(clientSchedulerBridge)
    private val authFailureRunnable = AuthFailureRunnable(clientSchedulerBridge)

    override fun connectMqtt() {
        connectMqtt(MQTT_WAIT_BEFORE_RECONNECT_TIME_MS)
    }

    override fun connectMqtt(timeMillis: Long) {
        try {
            sendThreadEventIfNotAlive()
            // make MQTT thread wait for time ms to attempt reconnect
            // remove any pending disconnect runnables before making any connection
            // also removing any handleMqttExceptionRunnable before making any connection;
            mqttThreadHandler.removeCallbacks(mqttExceptionRunnable)
            mqttThreadHandler.removeCallbacks(disconnectRunnable)
            mqttThreadHandler.postDelayed(connectionCheckRunnable, timeMillis)
        } catch (e: Exception) {
            logger.e(TAG, "Exception in MQTT connect handler", e)
        }
    }

    override fun disconnectMqtt(reconnect: Boolean, clearState: Boolean) {
        try {
            sendThreadEventIfNotAlive()
            disconnectRunnable.setReconnect(reconnect)
            disconnectRunnable.setClearState(clearState)
            // remove any pending disconnects queued
            mqttThreadHandler.removeCallbacks(disconnectRunnable)
            // remove any pending connects queued
            mqttThreadHandler.removeCallbacks(connectionCheckRunnable)
            mqttThreadHandler.postAtFrontOfQueue(disconnectRunnable)
        } catch (e: Exception) {
            logger.e(TAG, "Exception in MQTT disconnect", e)
        }
    }

    override fun scheduleNextActivityCheck() {
        try {
            sendThreadEventIfNotAlive()
            mqttThreadHandler.removeCallbacks(activityCheckRunnable)
            mqttThreadHandler.postDelayed(
                activityCheckRunnable,
                activityCheckIntervalSeconds * 1000.toLong()
            )
        } catch (e: Exception) {
            logger.e(TAG, "Exception scheduleNextActivityCheck", e)
        }
    }

    override fun scheduleMqttHandleExceptionRunnable(e: Exception?, reconnect: Boolean) {
        try {
            sendThreadEventIfNotAlive()
            mqttExceptionRunnable.setParameters(e, reconnect)
            mqttThreadHandler.removeCallbacks(mqttExceptionRunnable)
            mqttThreadHandler.removeCallbacks(connectionCheckRunnable)
            mqttThreadHandler.postAtFrontOfQueue(mqttExceptionRunnable)
        } catch (ex: Exception) {
            logger.e(TAG, "Exception while posting mqttdisconnect runnable", ex)
        }
    }

    override fun scheduleNextConnectionCheck() {
        scheduleNextConnectionCheck(ConnectRetryTimeConfig.MAX_RECONNECT_TIME_DEFAULT.toLong())
    }

    override fun scheduleNextConnectionCheck(reconnectTimeSecs: Long) {
        try {
            sendThreadEventIfNotAlive()
            mqttThreadHandler.removeCallbacks(connectionCheckRunnable)
            mqttThreadHandler.postDelayed(
                connectionCheckRunnable,
                reconnectTimeSecs * 1000
            )
        } catch (e: Exception) {
            logger.e(TAG, "Exception scheduleNextConnectionCheck", e)
        }
    }

    override fun scheduleSubscribe(delayMillis: Long, topicMap: Map<String, QoS>) {
        try {
            sendThreadEventIfNotAlive()
            mqttThreadHandler.postDelayed(
                SubscribeRunnable(clientSchedulerBridge, topicMap),
                delayMillis
            )
        } catch (ex: Exception) {
            logger.e(TAG, "Exception scheduleSubscribe", ex)
        }
    }

    override fun scheduleUnsubscribe(delayMillis: Long, topics: Set<String>) {
        try {
            sendThreadEventIfNotAlive()
            mqttThreadHandler.postDelayed(
                UnsubscribeRunnable(clientSchedulerBridge, topics),
                delayMillis
            )
        } catch (ex: Exception) {
            logger.e(TAG, "Exception scheduleUnsubscribe", ex)
        }
    }

    override fun scheduleResetParams(delayMillis: Long) {
        try {
            sendThreadEventIfNotAlive()
            mqttThreadHandler.removeCallbacks(resetParamsRunnable)
            mqttThreadHandler.postDelayed(resetParamsRunnable, delayMillis)
        } catch (ex: Exception) {
            logger.e(TAG, "Exception scheduleResetParams", ex)
        }
    }

    override fun scheduleAuthFailureRunnable(delayMillis: Long) {
        try {
            sendThreadEventIfNotAlive()
            mqttThreadHandler.removeCallbacks(authFailureRunnable)
            mqttThreadHandler.postDelayed(authFailureRunnable, delayMillis)
        } catch (ex: Exception) {
            logger.e(TAG, "Exception while scheduleAuthFailureRunnable", ex)
        }
    }

    override fun stopThread() {
        handlerThread.quitSafely()
    }

    private fun sendThreadEventIfNotAlive() {
        if (handlerThread.isAlive.not()) {
            eventHandler.onEvent(
                HandlerThreadNotAliveEvent(
                    isInterrupted = handlerThread.isInterrupted,
                    state = handlerThread.state
                )
            )
        }
    }

    companion object {
        const val TAG = "MqttRunnableScheduler"
    }
}
