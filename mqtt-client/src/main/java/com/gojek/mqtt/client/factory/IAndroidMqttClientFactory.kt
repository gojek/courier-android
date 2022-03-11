package com.gojek.mqtt.client.factory

import android.content.Context
import com.gojek.appstatemanager.AppStateManager
import com.gojek.courier.logging.NoOpLogger
import com.gojek.keepalive.KeepAliveFailureHandler
import com.gojek.mqtt.client.config.v3.MqttV3Configuration
import com.gojek.mqtt.client.internal.KeepAliveProvider
import com.gojek.mqtt.client.v3.IAndroidMqttClient
import com.gojek.mqtt.client.v3.impl.AndroidMqttClient
import com.gojek.mqtt.event.EventHandler
import com.gojek.mqtt.event.NoOpEventHandler
import com.gojek.mqtt.pingsender.IPingSenderEvents
import com.gojek.mqtt.pingsender.MqttPingSender
import com.gojek.networktracker.NetworkStateTracker

internal interface IAndroidMqttClientFactory {
    fun createAndroidMqttClient(
        context: Context,
        mqttConfiguration: MqttV3Configuration,
        networkStateTracker: NetworkStateTracker,
        appStateManager: AppStateManager,
        keepAliveProvider: KeepAliveProvider,
        keepAliveFailureHandler: KeepAliveFailureHandler,
        eventHandler: EventHandler,
        pingEventHandler: IPingSenderEvents
    ): IAndroidMqttClient
    fun createAdaptiveAndroidMqttClient(
        pingSender: MqttPingSender,
        context: Context,
        mqttConfiguration: MqttV3Configuration,
        networkStateTracker: NetworkStateTracker,
        appStateManager: AppStateManager,
        keepAliveProvider: KeepAliveProvider,
        keepAliveFailureHandler: KeepAliveFailureHandler,
        pingEventHandler: IPingSenderEvents
    ): IAndroidMqttClient
}

internal class AndroidMqttClientFactory : IAndroidMqttClientFactory {
    override fun createAndroidMqttClient(
        context: Context,
        mqttConfiguration: MqttV3Configuration,
        networkStateTracker: NetworkStateTracker,
        appStateManager: AppStateManager,
        keepAliveProvider: KeepAliveProvider,
        keepAliveFailureHandler: KeepAliveFailureHandler,
        eventHandler: EventHandler,
        pingEventHandler: IPingSenderEvents
    ): IAndroidMqttClient {
        val pingSender = mqttConfiguration.pingSender
        pingSender.setPingEventHandler(pingEventHandler)
        return AndroidMqttClient(
            context = context,
            mqttConfiguration = mqttConfiguration.copy(
                eventHandler = eventHandler
            ),
            networkStateTracker = networkStateTracker,
            appStateManager = appStateManager,
            keepAliveProvider = keepAliveProvider,
            keepAliveFailureHandler = keepAliveFailureHandler,
            mqttPingSender = pingSender
        )
    }

    override fun createAdaptiveAndroidMqttClient(
        pingSender: MqttPingSender,
        context: Context,
        mqttConfiguration: MqttV3Configuration,
        networkStateTracker: NetworkStateTracker,
        appStateManager: AppStateManager,
        keepAliveProvider: KeepAliveProvider,
        keepAliveFailureHandler: KeepAliveFailureHandler,
        pingEventHandler: IPingSenderEvents
    ): IAndroidMqttClient {
        pingSender.setPingEventHandler(pingEventHandler)
        return AndroidMqttClient(
            context = context,
            mqttConfiguration = mqttConfiguration.copy(
                logger = NoOpLogger(),
                eventHandler = NoOpEventHandler()
            ),
            networkStateTracker = networkStateTracker,
            appStateManager = appStateManager,
            mqttPingSender = pingSender,
            isAdaptiveKAConnection = true,
            keepAliveProvider = keepAliveProvider,
            keepAliveFailureHandler = keepAliveFailureHandler
        )
    }
}

internal fun getAndroidMqttClientFactory(): IAndroidMqttClientFactory {
    return AndroidMqttClientFactory()
}