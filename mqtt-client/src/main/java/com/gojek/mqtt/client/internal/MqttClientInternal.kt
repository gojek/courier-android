package com.gojek.mqtt.client.internal

import android.content.Context
import com.gojek.courier.QoS
import com.gojek.courier.callback.SendMessageCallback
import com.gojek.keepalive.KeepAliveFailureHandler
import com.gojek.keepalive.NoOpKeepAliveFailureHandler
import com.gojek.keepalive.OptimalKeepAliveFailureHandler
import com.gojek.keepalive.OptimalKeepAliveObserver
import com.gojek.keepalive.OptimalKeepAliveProvider
import com.gojek.keepalive.config.AdaptiveKeepAliveConfig as AdaptiveKAConfig
import com.gojek.mqtt.client.config.v3.MqttV3Configuration
import com.gojek.mqtt.client.event.interceptor.MqttEventHandler
import com.gojek.mqtt.client.factory.getAndroidMqttClientFactory
import com.gojek.mqtt.client.internal.State.INITIALISED
import com.gojek.mqtt.client.internal.State.UNINITIALISED
import com.gojek.mqtt.client.listener.MessageListener
import com.gojek.mqtt.client.model.ConnectionState
import com.gojek.mqtt.client.v3.IAndroidMqttClient
import com.gojek.mqtt.event.AdaptivePingEventHandler
import com.gojek.mqtt.event.EventHandler
import com.gojek.mqtt.event.MqttEvent.OperationDiscardedEvent
import com.gojek.mqtt.event.MqttEvent.OptimalKeepAliveFoundEvent
import com.gojek.mqtt.event.PingEventHandler
import com.gojek.mqtt.model.AdaptiveKeepAliveConfig
import com.gojek.mqtt.model.MqttConnectOptions
import com.gojek.mqtt.model.MqttPacket
import com.gojek.mqtt.utils.MqttUtils
import com.gojek.networktracker.NetworkStateTrackerFactory

internal class MqttClientInternal(
    private val context: Context,
    private val mqttConfiguration: MqttV3Configuration
) {
    private val androidMqttClientFactory = getAndroidMqttClientFactory()

    private val networkStateTracker =
        NetworkStateTrackerFactory.create(context.applicationContext, mqttConfiguration.logger)

    private var androidMqttClient: IAndroidMqttClient? = null

    private var optimalKeepAliveProvider: OptimalKeepAliveProvider? = null

    private var adaptiveMqttClient: IAndroidMqttClient? = null

    private var keepAliveProvider: KeepAliveProvider = NonAdaptiveKeepAliveProvider()
    private var keepAliveFailureHandler: KeepAliveFailureHandler = NoOpKeepAliveFailureHandler()

    private val eventHandler = MqttEventHandler(MqttUtils())

    private var initialisationState = UNINITIALISED

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
        initialisationState = INITIALISED
    }

    @Synchronized
    fun connect(connectOptions: MqttConnectOptions) {
        if (initialisationState == UNINITIALISED) {
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
            initialisationState = INITIALISED
        }
        androidMqttClient?.connect(connectOptions)
        adaptiveMqttClient?.connect(connectOptions)
    }

    @Synchronized
    fun disconnect() {
        if (initialisationState == UNINITIALISED) {
            mqttConfiguration.logger.d("MqttClient", "MqttClient is not initialised")
            eventHandler.onEvent(OperationDiscardedEvent("Disconnect", "State uninitialised"))
            return
        }
        androidMqttClient?.disconnect()
        adaptiveMqttClient?.disconnect()
    }

    @Synchronized
    fun destroy() {
        if (initialisationState == UNINITIALISED) {
            mqttConfiguration.logger.d("MqttClient", "MqttClient is not initialised")
            eventHandler.onEvent(OperationDiscardedEvent("Destroy", "State uninitialised"))
            return
        }
        androidMqttClient?.destroy()
        adaptiveMqttClient?.destroy()
        if (mqttConfiguration.experimentConfigs.cleanMqttClientOnDestroy) {
            initialisationState = UNINITIALISED
        }
    }

    @Synchronized
    fun reconnect() {
        if (initialisationState == UNINITIALISED) {
            mqttConfiguration.logger.d("MqttClient", "MqttClient is not initialised")
            eventHandler.onEvent(OperationDiscardedEvent("Reconnect", "State uninitialised"))
            return
        }
        androidMqttClient?.reconnect()
        adaptiveMqttClient?.reconnect()
    }

    @Synchronized
    fun subscribe(vararg topics: Pair<String, QoS>) {
        if (initialisationState == UNINITIALISED) {
            mqttConfiguration.logger.d("MqttClient", "MqttClient is not initialised")
            eventHandler.onEvent(OperationDiscardedEvent("Subscribe", "State uninitialised"))
            return
        }
        androidMqttClient?.subscribe(mapOf(*topics))
    }

    @Synchronized
    fun unsubscribe(vararg topics: String) {
        if (initialisationState == UNINITIALISED) {
            mqttConfiguration.logger.d("MqttClient", "MqttClient is not initialised")
            eventHandler.onEvent(OperationDiscardedEvent("Unsubscribe", "State uninitialised"))
            return
        }
        androidMqttClient?.unsubscribe(listOf(*topics))
    }

    @Synchronized
    fun send(mqttPacket: MqttPacket, sendMessageCallback: SendMessageCallback): Boolean {
        if (initialisationState == UNINITIALISED) {
            mqttConfiguration.logger.d("MqttClient", "MqttClient is not initialised")
            eventHandler.onEvent(OperationDiscardedEvent("SendMessage", "State uninitialised"))
            return false
        }
        return androidMqttClient?.send(mqttPacket, sendMessageCallback) ?: false
    }

    @Synchronized
    fun addMessageListener(topic: String, listener: MessageListener) {
        if (initialisationState == UNINITIALISED) {
            mqttConfiguration.logger.d("MqttClient", "MqttClient is not initialised")
            eventHandler.onEvent(OperationDiscardedEvent("AddMessageListener", "State uninitialised"))
            return
        }
        androidMqttClient?.addMessageListener(topic, listener)
    }

    @Synchronized
    fun removeMessageListener(topic: String, listener: MessageListener) {
        if (initialisationState == UNINITIALISED) {
            mqttConfiguration.logger.d("MqttClient", "MqttClient is not initialised")
            eventHandler.onEvent(OperationDiscardedEvent("RemoveMessageListener", "State uninitialised"))
            return
        }
        androidMqttClient?.removeMessageListener(topic, listener)
    }

    @Synchronized
    fun addGlobalMessageListener(listener: MessageListener) {
        if (initialisationState == UNINITIALISED) {
            mqttConfiguration.logger.d("MqttClient", "MqttClient is not initialised")
            eventHandler.onEvent(OperationDiscardedEvent("AddGlobalMessageListener", "State uninitialised"))
            return
        }
        androidMqttClient?.addGlobalMessageListener(listener)
    }

    @Synchronized
    fun getCurrentState(): ConnectionState {
        if (initialisationState == UNINITIALISED) {
            mqttConfiguration.logger.d("MqttClient", "MqttClient is not initialised")
            return ConnectionState.UNINITIALISED
        }
        return androidMqttClient?.getCurrentState() ?: ConnectionState.UNINITIALISED
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

private enum class State {
    UNINITIALISED, INITIALISED
}
