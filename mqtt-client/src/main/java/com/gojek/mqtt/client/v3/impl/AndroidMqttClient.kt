package com.gojek.mqtt.client.v3.impl

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.RemoteException
import androidx.annotation.RequiresApi
import com.gojek.courier.QoS
import com.gojek.courier.exception.AuthApiException
import com.gojek.courier.extensions.fromNanosToMillis
import com.gojek.courier.logging.ILogger
import com.gojek.courier.utils.Clock
import com.gojek.keepalive.KeepAliveFailureHandler
import com.gojek.mqtt.client.IClientSchedulerBridge
import com.gojek.mqtt.client.IMessageReceiveListener
import com.gojek.mqtt.client.IncomingMsgController
import com.gojek.mqtt.client.IncomingMsgControllerImpl
import com.gojek.mqtt.client.config.SubscriptionStore.IN_MEMORY
import com.gojek.mqtt.client.config.SubscriptionStore.PERSISTABLE
import com.gojek.mqtt.client.config.SubscriptionStore.PERSISTABLE_V2
import com.gojek.mqtt.client.config.v3.MqttV3Configuration
import com.gojek.mqtt.client.connectioninfo.ConnectionInfo
import com.gojek.mqtt.client.connectioninfo.ConnectionInfoStore
import com.gojek.mqtt.client.event.adapter.MqttClientEventAdapter
import com.gojek.mqtt.client.internal.KeepAliveProvider
import com.gojek.mqtt.client.listener.MessageListener
import com.gojek.mqtt.client.mapToPahoInterceptor
import com.gojek.mqtt.client.model.ConnectionState
import com.gojek.mqtt.client.model.ConnectionState.CONNECTED
import com.gojek.mqtt.client.model.ConnectionState.CONNECTING
import com.gojek.mqtt.client.model.ConnectionState.DISCONNECTED
import com.gojek.mqtt.client.model.ConnectionState.DISCONNECTING
import com.gojek.mqtt.client.model.ConnectionState.INITIALISED
import com.gojek.mqtt.client.model.MqttSendPacket
import com.gojek.mqtt.client.v3.IAndroidMqttClient
import com.gojek.mqtt.connection.IMqttConnection
import com.gojek.mqtt.connection.MqttConnection
import com.gojek.mqtt.connection.config.v3.ConnectionConfig
import com.gojek.mqtt.constants.MAX_INFLIGHT_MESSAGES_ALLOWED
import com.gojek.mqtt.constants.MESSAGE
import com.gojek.mqtt.constants.MSG_APP_PUBLISH
import com.gojek.mqtt.event.EventHandler
import com.gojek.mqtt.event.MqttEvent.AuthenticatorErrorEvent
import com.gojek.mqtt.event.MqttEvent.MqttConnectDiscardedEvent
import com.gojek.mqtt.event.MqttEvent.MqttConnectFailureEvent
import com.gojek.mqtt.event.MqttEvent.MqttDisconnectEvent
import com.gojek.mqtt.event.MqttEvent.MqttMessageReceiveErrorEvent
import com.gojek.mqtt.event.MqttEvent.MqttMessageReceiveEvent
import com.gojek.mqtt.event.MqttEvent.MqttMessageSendEvent
import com.gojek.mqtt.event.MqttEvent.MqttMessageSendFailureEvent
import com.gojek.mqtt.event.MqttEvent.MqttMessageSendSuccessEvent
import com.gojek.mqtt.event.MqttEvent.MqttReconnectEvent
import com.gojek.mqtt.exception.toCourierException
import com.gojek.mqtt.handler.IncomingHandler
import com.gojek.mqtt.model.MqttConnectOptions
import com.gojek.mqtt.model.MqttPacket
import com.gojek.mqtt.network.NetworkHandler
import com.gojek.mqtt.persistence.impl.PahoPersistence
import com.gojek.mqtt.persistence.model.MqttReceivePacket
import com.gojek.mqtt.persistence.model.toMqttMessage
import com.gojek.mqtt.pingsender.MqttPingSender
import com.gojek.mqtt.policies.hostfallback.HostFallbackPolicy
import com.gojek.mqtt.policies.hostfallback.IHostFallbackPolicy
import com.gojek.mqtt.scheduler.IRunnableScheduler
import com.gojek.mqtt.scheduler.MqttRunnableScheduler
import com.gojek.mqtt.send.listener.IMessageSendListener
import com.gojek.mqtt.subscription.InMemorySubscriptionStore
import com.gojek.mqtt.subscription.PersistableSubscriptionStore
import com.gojek.mqtt.subscription.PersistableSubscriptionStoreV2
import com.gojek.mqtt.subscription.SubscriptionStore
import com.gojek.mqtt.utils.MqttUtils
import com.gojek.mqtt.utils.NetworkUtils
import com.gojek.mqtt.wakelock.WakeLockProvider
import com.gojek.networktracker.NetworkStateTracker
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttException.REASON_CODE_UNEXPECTED_ERROR
import org.eclipse.paho.client.mqttv3.MqttPersistenceException

internal class AndroidMqttClient(
    private val context: Context,
    private val mqttConfiguration: MqttV3Configuration,
    private val networkStateTracker: NetworkStateTracker,
    private val mqttPingSender: MqttPingSender,
    private val isAdaptiveKAConnection: Boolean = false,
    private val keepAliveProvider: KeepAliveProvider,
    private val eventHandler: EventHandler,
    keepAliveFailureHandler: KeepAliveFailureHandler
) : IAndroidMqttClient, IClientSchedulerBridge {

    private val runnableScheduler: IRunnableScheduler
    private val mqttConnection: IMqttConnection
    private val mqttThreadLooper: Looper
    private val mqttThreadHandler: Handler
    private var mMessenger: Messenger
    private val networkUtils: NetworkUtils
    private val mqttUtils: MqttUtils
    private val mqttPersistence: PahoPersistence
    private val messageSendListener: IMessageSendListener
    private val networkHandler: NetworkHandler
    private val mqttClientEventAdapter: MqttClientEventAdapter
    private val logger: ILogger
    private val incomingMsgController: IncomingMsgController

    private lateinit var connectOptions: MqttConnectOptions

    private val experimentConfigs = mqttConfiguration.experimentConfigs

    @Volatile
    private var globalListener: MessageListener? = null

    @Volatile
    private var isInitialised = false

    // Accessed only from mqtt thread
    private var forceRefresh = false

    private val subscriptionStore: SubscriptionStore =
        when (experimentConfigs.subscriptionStore) {
            IN_MEMORY -> InMemorySubscriptionStore()
            PERSISTABLE -> PersistableSubscriptionStore(context)
            PERSISTABLE_V2 -> PersistableSubscriptionStoreV2(context)
        }

    private var hostFallbackPolicy: IHostFallbackPolicy? = null

    private val clock = Clock()

    init {
        logger = mqttConfiguration.logger
        val mqttHandlerThread = HandlerThread("MQTT_Thread")
        mqttHandlerThread.start()
        mqttThreadLooper = mqttHandlerThread.looper
        mqttThreadHandler = Handler(mqttThreadLooper)
        mMessenger = Messenger(
            IncomingHandler(mqttThreadLooper, this, logger)
        )
        @RequiresApi
        runnableScheduler = MqttRunnableScheduler(
            mqttHandlerThread,
            mqttThreadHandler,
            this,
            logger,
            eventHandler,
            experimentConfigs.activityCheckIntervalSeconds
        )
        mqttUtils = MqttUtils()
        networkUtils = NetworkUtils()
        mqttPersistence = PahoPersistence(context)
        messageSendListener = MqttMessageSendListener()
        networkHandler = NetworkHandler(
            logger = mqttConfiguration.logger,
            androidMqttClient = this,
            networkUtils = networkUtils,
            networkStateTracker = networkStateTracker
        )
        mqttClientEventAdapter = MqttClientEventAdapter(
            eventHandler = eventHandler,
            networkHandler = networkHandler
        )
        val connectionConfig =
            ConnectionConfig(
                connectRetryTimePolicy = mqttConfiguration.connectRetryTimePolicy,
                connectTimeoutPolicy = mqttConfiguration.connectTimeoutPolicy,
                subscriptionRetryPolicy = mqttConfiguration.subscriptionRetryPolicy,
                unsubscriptionRetryPolicy = mqttConfiguration.unsubscriptionRetryPolicy,
                wakeLockTimeout = mqttConfiguration.wakeLockTimeout,
                maxInflightMessages = MAX_INFLIGHT_MESSAGES_ALLOWED,
                logger = mqttConfiguration.logger,
                connectionEventHandler = mqttClientEventAdapter.adapt(),
                mqttInterceptorList = mqttConfiguration.mqttInterceptorList.map {
                    mapToPahoInterceptor(it)
                },
                persistenceOptions = mqttConfiguration.persistenceOptions,
                inactivityTimeoutSeconds = experimentConfigs.inactivityTimeoutSeconds,
                policyResetTimeSeconds = experimentConfigs.policyResetTimeSeconds,
                shouldUseNewSSLFlow = experimentConfigs.shouldUseNewSSLFlow
            )

        mqttConnection = MqttConnection(
            context = context,
            connectionConfig = connectionConfig,
            runnableScheduler = runnableScheduler,
            networkUtils = networkUtils,
            wakeLockProvider = WakeLockProvider(context, logger),
            messageSendListener = messageSendListener,
            pahoPersistence = mqttPersistence,
            networkHandler = networkHandler,
            mqttPingSender = getMqttPingSender(),
            keepAliveFailureHandler = keepAliveFailureHandler,
            clock = clock,
            subscriptionStore = subscriptionStore
        )
        incomingMsgController = IncomingMsgControllerImpl(
            mqttUtils,
            mqttPersistence,
            logger,
            eventHandler,
            experimentConfigs.incomingMessagesTTLSecs,
            experimentConfigs.incomingMessagesCleanupIntervalSecs,
            clock
        )
        networkHandler.init()
    }

    // This can be invoked on any thread
    override fun connect(
        connectOptions: MqttConnectOptions
    ) {
        this.connectOptions = connectOptions
        isInitialised = true
        runnableScheduler.connectMqtt()
    }

    // This can be invoked on any thread
    override fun reconnect() {
        eventHandler.onEvent(MqttReconnectEvent())
        runnableScheduler.disconnectMqtt(true)
    }

    // This can be invoked on any thread
    override fun disconnect(clearState: Boolean) {
        isInitialised = false
        runnableScheduler.disconnectMqtt(false, clearState)
    }

    // This can be invoked on any thread
    override fun connect(timeMillis: Long) {
        runnableScheduler.connectMqtt(timeMillis)
    }

    // This runs on Mqtt thread
    override fun sendMessage(mqttPacket: MqttSendPacket) {
        if (!isConnected()) {
            connectMqtt()
        }

        try {
            logger.d(
                TAG,
                "Publishing mqtt packet on ${mqttPacket.topic} " +
                    "with qos ${mqttPacket.qos}"
            )
            with(mqttPacket) {
                eventHandler.onEvent(
                    MqttMessageSendEvent(topic, qos, message.size)
                )
            }
            mqttConnection.publish(mqttPacket, mqttPacket.qos, mqttPacket.type, mqttPacket.topic)
        } catch (e: MqttPersistenceException) {
            with(mqttPacket) {
                eventHandler.onEvent(
                    MqttMessageSendFailureEvent(
                        topic = topic,
                        qos = qos,
                        sizeBytes = message.size,
                        exception = e.toCourierException()
                    )
                )
            }
        } catch (e: MqttException) {
            with(mqttPacket) {
                eventHandler.onEvent(
                    MqttMessageSendFailureEvent(
                        topic = topic,
                        qos = qos,
                        sizeBytes = message.size,
                        exception = e.toCourierException()
                    )
                )
            }
            runnableScheduler.scheduleMqttHandleExceptionRunnable(e, true)
        } catch (e: java.lang.Exception) {
            // this might happen if mqtt object becomes null while disconnect, so just ignore
            with(mqttPacket) {
                eventHandler.onEvent(
                    MqttMessageSendFailureEvent(
                        topic = topic,
                        qos = qos,
                        sizeBytes = message.size,
                        exception = e.toCourierException()
                    )
                )
            }
        }
    }

    // This can be invoked on any thread
    override fun send(mqttPacket: MqttPacket): Boolean {
        val mqttSendPacket = MqttSendPacket(
            mqttPacket.message,
            0,
            System.currentTimeMillis(),
            mqttPacket.qos.value,
            mqttPacket.topic,
            mqttPacket.qos.type
        )

        val msg = Message.obtain()
        msg.what = MSG_APP_PUBLISH

        val bundle = Bundle()
        bundle.putParcelable(MESSAGE, mqttSendPacket)

        msg.data = bundle
        msg.replyTo = mMessenger

        try {
            mMessenger.send(msg)
        } catch (e: RemoteException) {
            /* Service is dead. What to do? */
            logger.e(TAG, "Remote Service dead", e)
            return false
        }

        return true
    }

    override fun addMessageListener(topic: String, listener: MessageListener) {
        incomingMsgController.registerListener(topic, listener)
    }

    override fun removeMessageListener(topic: String, listener: MessageListener) {
        incomingMsgController.unregisterListener(topic, listener)
    }

    override fun addGlobalMessageListener(listener: MessageListener) {
        this.globalListener = listener
    }

    // This runs on Mqtt thread
    override fun connectMqtt() {
        val startTime = clock.nanoTime()
        try {
            logger.d(TAG, "Sending onConnectAttempt event")
            if (!isInitialised) {
                logger.d(TAG, "Mqtt Client not initialised")
                eventHandler.onEvent(
                    MqttConnectDiscardedEvent(
                        "Mqtt Client not initialised",
                        networkHandler.getActiveNetworkInfo()
                    )
                )
                return
            }

            if (mqttConfiguration.authFailureHandler == null) {
                try {
                    connectOptions =
                        mqttConfiguration.authenticator.authenticate(connectOptions, forceRefresh)
                } catch (ex: AuthApiException) {
                    throw ex
                } catch (th: Throwable) {
                    throw AuthApiException(nextRetrySeconds = 0, failureCause = th)
                }
            }

            val processedConnectOptions = postProcessConnectOptions(connectOptions)

            mqttConnection.connect(
                processedConnectOptions,
                MqttMessageReceiverListener(),
                hostFallbackPolicy!!,
                subscriptionStore.getSubscribeTopics()
            )
        } catch (e: AuthApiException) /* this exception can be thrown by authenticator */ {
            logger.e(TAG, "Auth exception : ${e.message}")
            forceRefresh = true
            eventHandler.onEvent(
                AuthenticatorErrorEvent(
                    exception = e.toCourierException(),
                    nextRetryTimeSecs = e.nextRetrySeconds,
                    activeNetworkInfo = networkHandler.getActiveNetworkInfo(),
                    timeTakenMillis = (clock.nanoTime() - startTime).fromNanosToMillis()
                )
            )
            if (e.nextRetrySeconds > 0) {
                runnableScheduler.connectMqtt(TimeUnit.SECONDS.toMillis(e.nextRetrySeconds))
            } else {
                val mqttException = MqttException(REASON_CODE_UNEXPECTED_ERROR.toInt(), e)
                runnableScheduler.scheduleMqttHandleExceptionRunnable(mqttException, true)
            }
        } catch (e: Exception) /* this exception cannot be thrown on connect */ {
            logger.e(TAG, "Connect exception : ${e.message}")
            eventHandler.onEvent(
                MqttConnectFailureEvent(
                    exception = e.toCourierException(),
                    activeNetInfo = networkHandler.getActiveNetworkInfo(),
                    serverUri = hostFallbackPolicy?.getServerUri(),
                    timeTakenMillis = (clock.nanoTime() - startTime).fromNanosToMillis()
                )
            )
            val mqttException = MqttException(REASON_CODE_UNEXPECTED_ERROR.toInt(), e)
            runnableScheduler.scheduleMqttHandleExceptionRunnable(mqttException, true)
        }
    }

    // This runs on Mqtt thread
    override fun disconnectMqtt(clearState: Boolean) {
        eventHandler.onEvent(MqttDisconnectEvent())
        mqttConnection.disconnect()
        if (clearState) {
            mqttConnection.shutDown()
            subscriptionStore.clear()
            mqttPersistence.clearAll()
        }
    }

    // This runs on Mqtt thread
    override fun handleMqttException(exception: Exception?, reconnect: Boolean) {
        mqttConnection.handleException(exception, reconnect)
    }

    // This runs on Mqtt thread
    override fun isConnected(): Boolean {
        return mqttConnection.isConnected()
    }

    override fun subscribe(topicMap: Map<String, QoS>) {
        val addedTopics = subscriptionStore.subscribeTopics(topicMap)
        runnableScheduler.scheduleSubscribe(0, addedTopics)
    }

    override fun unsubscribe(topics: List<String>) {
        val removedTopics = subscriptionStore.unsubscribeTopics(topics)
        runnableScheduler.scheduleUnsubscribe(0, removedTopics)
    }

    override fun getCurrentState(): ConnectionState {
        return when {
            mqttConnection.isConnecting() -> {
                CONNECTING
            }
            mqttConnection.isConnected() -> {
                CONNECTED
            }
            mqttConnection.isDisconnecting() -> {
                DISCONNECTING
            }
            mqttConnection.isDisconnected() -> {
                DISCONNECTED
            }
            else -> {
                INITIALISED
            }
        }
    }

    // This runs on Mqtt thread
    override fun isConnecting(): Boolean {
        return mqttConnection.isConnecting()
    }

    // This runs on Mqtt thread
    override fun checkActivity() {
        mqttConnection.checkActivity()
    }

    // This can be invoked on any thread
    override fun scheduleNextActivityCheck() {
        runnableScheduler.scheduleNextActivityCheck()
    }

    // This runs on Mqtt thread
    override fun subscribeMqtt(topicMap: Map<String, QoS>) {
        if (mqttConnection.isConnected()) {
            mqttConnection.subscribe(topicMap)
        }
    }

    // This runs on Mqtt thread
    override fun unsubscribeMqtt(topics: Set<String>) {
        if (mqttConnection.isConnected()) {
            mqttConnection.unsubscribe(topics)
        }
    }

    // This runs on Mqtt thread
    override fun resetParams() {
        mqttConnection.resetParams()
    }

    // This runs on Mqtt thread
    override fun handleAuthFailure() {
        with(mqttConfiguration) {
            if (authFailureHandler == null) {
                forceRefresh = true
                connectMqtt()
            } else {
                authFailureHandler.handleAuthFailure()
            }
        }
    }

    private fun triggerHandleMessage() {
        incomingMsgController.triggerHandleMessage()
    }

    private fun getMqttPingSender(): MqttPingSender {
        return mqttPingSender
    }

    private fun postProcessConnectOptions(connectOptions: MqttConnectOptions): MqttConnectOptions {
        forceRefresh = false
        this.hostFallbackPolicy = HostFallbackPolicy(connectOptions.serverUris)
        val mqttConnectOptions = if (isAdaptiveKAConnection) {
            connectOptions.newBuilder()
                .keepAlive(keepAliveProvider.getKeepAlive(connectOptions))
                .clientId(connectOptions.clientId + ":adaptive")
                .cleanSession(true)
                .build()
        } else {
            connectOptions.newBuilder()
                .keepAlive(keepAliveProvider.getKeepAlive(connectOptions))
                .build()
        }

        if (isAdaptiveKAConnection.not()) {
            ConnectionInfoStore.updateConnectionInfo(
                ConnectionInfo(
                    clientId = mqttConnectOptions.clientId,
                    username = mqttConnectOptions.username,
                    keepaliveSeconds = mqttConnectOptions.keepAlive.timeSeconds,
                    connectTimeout = mqttConfiguration.connectTimeoutPolicy.getConnectTimeOut(),
                    host = hostFallbackPolicy!!.getServerUri().host,
                    port = hostFallbackPolicy!!.getServerUri().port,
                    scheme = hostFallbackPolicy!!.getServerUri().scheme
                )
            )
        }
        return mqttConnectOptions
    }

    inner class MqttMessageReceiverListener :
        IMessageReceiveListener {
        override fun messageArrived(topic: String, byteArray: ByteArray): Boolean {
            try {
                eventHandler.onEvent(
                    MqttMessageReceiveEvent(topic, byteArray.size)
                )
                val bytes = mqttUtils.uncompressByteArray(byteArray)!!
                val messageBody = String(bytes, StandardCharsets.UTF_8)
                val logMsg = "messageArrived called for message code : "
                logger.i(TAG, logMsg + messageBody)
                val mqttPacket =
                    MqttReceivePacket(
                        bytes,
                        0,
                        clock.nanoTime(),
                        topic
                    )
                mqttPersistence.addReceivedMessage(mqttPacket)
                globalListener?.onMessageReceived(mqttPacket.toMqttMessage())
                triggerHandleMessage()
            } catch (e: IllegalStateException) {
                eventHandler.onEvent(
                    MqttMessageReceiveErrorEvent(topic, byteArray.size, e.toCourierException())
                )
                logger.e(TAG, "Exception when msg arrived : ", e)
                runnableScheduler.disconnectMqtt(true)
                return false
            } catch (e: Throwable) {
                eventHandler.onEvent(
                    MqttMessageReceiveErrorEvent(topic, byteArray.size, e.toCourierException())
                )
                logger.e(TAG, "Exception when msg arrived : ", e)
            }
            return true
        }
    }

    inner class MqttMessageSendListener :
        IMessageSendListener {
        override fun onSuccess(packet: MqttSendPacket) {
            with(packet) {
                eventHandler.onEvent(
                    MqttMessageSendSuccessEvent(
                        topic,
                        qos,
                        message.size
                    )
                )
            }
        }

        override fun onFailure(packet: MqttSendPacket, exception: Throwable) {
            runnableScheduler.connectMqtt()
        }

        override fun notifyWrittenOnSocket(packet: MqttSendPacket) {}
    }

    companion object {
        const val TAG = "AndroidMqttClient"
    }
}
