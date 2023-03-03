package com.gojek.mqtt.client.internal

import android.content.Context
import com.gojek.courier.QoS
import com.gojek.keepalive.KeepAliveFailureHandler
import com.gojek.keepalive.NoOpKeepAliveFailureHandler
import com.gojek.keepalive.OptimalKeepAliveFailureHandler
import com.gojek.keepalive.OptimalKeepAliveObserver
import com.gojek.keepalive.OptimalKeepAliveProvider
import com.gojek.keepalive.config.AdaptiveKeepAliveConfig as AdaptiveKAConfig
import com.gojek.mqtt.client.config.v3.MqttV3Configuration
import com.gojek.mqtt.client.event.interceptor.MqttEventHandler
import com.gojek.mqtt.client.factory.getAndroidMqttClientFactory
import com.gojek.mqtt.client.listener.MessageListener
import com.gojek.mqtt.client.model.ConnectionState
import com.gojek.mqtt.client.v3.IAndroidMqttClient
import com.gojek.mqtt.event.AdaptivePingEventHandler
import com.gojek.mqtt.event.EventHandler
import com.gojek.mqtt.event.MqttEvent.OptimalKeepAliveFoundEvent
import com.gojek.mqtt.event.PingEventHandler
import com.gojek.mqtt.model.AdaptiveKeepAliveConfig
import com.gojek.mqtt.model.MqttConnectOptions
import com.gojek.mqtt.model.MqttPacket
import com.gojek.networktracker.NetworkStateTrackerFactory

internal class MqttClientInternal(
    private val context: Context,
    private val mqttConfiguration: MqttV3Configuration
) {
    private val androidMqttClientFactory = getAndroidMqttClientFactory()

    private val networkStateTracker =
        NetworkStateTrackerFactory.create(context.applicationContext, mqttConfiguration.logger)

    private val androidMqttClient: IAndroidMqttClient

    private var optimalKeepAliveProvider: OptimalKeepAliveProvider? = null

    private var adaptiveMqttClient: IAndroidMqttClient? = null

    private var keepAliveProvider: KeepAliveProvider = NonAdaptiveKeepAliveProvider()
    private var keepAliveFailureHandler: KeepAliveFailureHandler = NoOpKeepAliveFailureHandler()

    private val eventHandler = MqttEventHandler()

    private val optimalKeepAliveObserver = object : OptimalKeepAliveObserver {
        override fun onOptimalKeepAliveFound(
            timeMinutes: Int,
            probeCount: Int,
            convergenceTime: Int
        ) {
            mqttConfiguration.logger.d("MqttClient", "Optimal KA found: $timeMinutes")
            eventHandler.onEvent(
                OptimalKeepAliveFoundEvent(
                    timeMinutes,
                    probeCount,
                    convergenceTime
                )
            )
            adaptiveMqttClient?.disconnect()
        }
    }

    init {
        initialiseAdaptiveMqttClient()
        androidMqttClient = androidMqttClientFactory.createAndroidMqttClient(
            context = context,
            mqttConfiguration = mqttConfiguration,
            networkStateTracker = networkStateTracker,
            keepAliveProvider = keepAliveProvider,
            keepAliveFailureHandler = keepAliveFailureHandler,
            eventHandler = eventHandler,
            pingEventHandler = PingEventHandler(eventHandler)
        )
    }

    fun connect(connectOptions: MqttConnectOptions) {
        androidMqttClient.connect(connectOptions)
        adaptiveMqttClient?.connect(connectOptions)
    }

    fun disconnect(clearState: Boolean) {
        androidMqttClient.disconnect(clearState)
        adaptiveMqttClient?.disconnect(clearState)
    }

    fun reconnect() {
        androidMqttClient.reconnect()
        adaptiveMqttClient?.reconnect()
    }

    fun subscribe(vararg topics: Pair<String, QoS>) {
        androidMqttClient.subscribe(mapOf(*topics))
    }

    fun unsubscribe(vararg topics: String) {
        androidMqttClient.unsubscribe(listOf(*topics))
    }

    fun send(mqttPacket: MqttPacket): Boolean {
        return androidMqttClient.send(mqttPacket)
    }

    fun addMessageListener(topic: String, listener: MessageListener) {
        return androidMqttClient.addMessageListener(topic, listener)
    }

    fun removeMessageListener(topic: String, listener: MessageListener) {
        return androidMqttClient.removeMessageListener(topic, listener)
    }

    fun addGlobalMessageListener(listener: MessageListener) {
        return androidMqttClient.addGlobalMessageListener(listener)
    }

    fun getCurrentState(): ConnectionState {
        return androidMqttClient.getCurrentState()
    }

    private fun initialiseAdaptiveMqttClient() {
        with(mqttConfiguration.experimentConfigs) {
            if (adaptiveKeepAliveConfig != null) {
                initialiseOptimalKeepAliveProvider(adaptiveKeepAliveConfig)
                val keepAliveSeconds =
                    optimalKeepAliveProvider!!.getOptimalKASecondsForCurrentNetwork()

                if (keepAliveSeconds != 0) { // Optimal keep alive is already known
                    keepAliveFailureHandler =
                        OptimalKeepAliveFailureHandler(optimalKeepAliveProvider!!)
                    keepAliveProvider = OptimalKeepAliveProvider(keepAliveSeconds)
                } else {
                    adaptiveMqttClient ?: kotlin.run {
                        adaptiveKeepAliveConfig.pingSender.setKeepAliveCalculator(
                            optimalKeepAliveProvider!!.keepAliveCalculator
                        )
                        adaptiveMqttClient =
                            androidMqttClientFactory.createAdaptiveAndroidMqttClient(
                                pingSender = adaptiveKeepAliveConfig.pingSender,
                                context = context,
                                mqttConfiguration = mqttConfiguration,
                                networkStateTracker = networkStateTracker,
                                keepAliveProvider = NonOptimalKeepAliveProvider(
                                    adaptiveKeepAliveConfig.upperBoundMinutes * 60
                                ),
                                keepAliveFailureHandler = NoOpKeepAliveFailureHandler(),
                                pingEventHandler = AdaptivePingEventHandler(eventHandler)
                            )
                    }
                }
            }
        }
    }

    private fun initialiseOptimalKeepAliveProvider(
        adaptiveKeepAliveConfig: AdaptiveKeepAliveConfig
    ) {
        optimalKeepAliveProvider ?: kotlin.run {
            optimalKeepAliveProvider = OptimalKeepAliveProvider(
                context = context,
                adaptiveKeepAliveConfig = AdaptiveKAConfig(
                    lowerBoundMinutes = adaptiveKeepAliveConfig.lowerBoundMinutes,
                    upperBoundMinutes = adaptiveKeepAliveConfig.upperBoundMinutes,
                    stepMinutes = adaptiveKeepAliveConfig.stepMinutes,
                    optimalKeepAliveResetLimit = adaptiveKeepAliveConfig.optimalKeepAliveResetLimit
                ),
                optimalKeepAliveObserver = optimalKeepAliveObserver
            )
        }
    }

    fun addEventHandler(eventHandler: EventHandler) {
        this.eventHandler.addEventHandler(eventHandler)
    }

    fun removeEventHandler(eventHandler: EventHandler) {
        this.eventHandler.removeEventHandler(eventHandler)
    }
}
