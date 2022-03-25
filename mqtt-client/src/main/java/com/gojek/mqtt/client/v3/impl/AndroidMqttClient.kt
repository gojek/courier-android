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
import com.gojek.appstatemanager.AppState
import com.gojek.appstatemanager.AppStateChangeListener
import com.gojek.appstatemanager.AppStateManager
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
import com.gojek.mqtt.client.model.MqttMessage
import com.gojek.mqtt.client.model.MqttSendPacket
import com.gojek.mqtt.client.v3.IAndroidMqttClient
import com.gojek.mqtt.connection.IMqttConnection
import com.gojek.mqtt.connection.MqttConnection
import com.gojek.mqtt.connection.config.v3.ConnectionConfig
import com.gojek.mqtt.constants.MAX_INFLIGHT_MESSAGES_ALLOWED
import com.gojek.mqtt.constants.MESSAGE
import com.gojek.mqtt.constants.MSG_APP_PUBLISH
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
import com.gojek.mqtt.pingsender.MqttPingSender
import com.gojek.mqtt.policies.hostfallback.HostFallbackPolicy
import com.gojek.mqtt.policies.hostfallback.IHostFallbackPolicy
import com.gojek.mqtt.scheduler.IRunnableScheduler
import com.gojek.mqtt.scheduler.MqttRunnableScheduler
import com.gojek.mqtt.send.listener.IMessageSendListener
import com.gojek.mqtt.subscription.InMemorySubscriptionStore
import com.gojek.mqtt.subscription.PersistableSubscriptionStore
import com.gojek.mqtt.subscription.SubscriptionStore
import com.gojek.mqtt.utils.MqttUtils
import com.gojek.mqtt.utils.NetworkUtils
import com.gojek.mqtt.wakelock.WakeLockProvider
import com.gojek.networktracker.NetworkStateTracker
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttException.REASON_CODE_UNEXPECTED_ERROR
import org.eclipse.paho.client.mqttv3.MqttPersistenceException
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

internal class AndroidMqttClient(
    private val context: Context,
    private val mqttConfiguration: MqttV3Configuration,
    private val networkStateTracker: NetworkStateTracker,
    private val appStateManager: AppStateManager,
    private val mqttPingSender: MqttPingSender,
    private val isAdaptiveKAConnection: Boolean = false,
    private val keepAliveProvider: KeepAliveProvider,
    keepAliveFailureHandler: KeepAliveFailureHandler
): IAndroidMqttClient, IClientSchedulerBridge {

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

    private val publishSubject = PublishSubject.create<MqttPacket>()
    private val compositeDisposable = CompositeDisposable()
    private val experimentConfigs = mqttConfiguration.experimentConfigs

    @Volatile
    private var isInitialised = false

    //Accessed only from mqtt thread
    private var forceRefresh = false

    private val subscriptionStore: SubscriptionStore =
        if (experimentConfigs.isPersistentSubscriptionStoreEnabled) {
            PersistableSubscriptionStore(context)
        } else {
            InMemorySubscriptionStore()
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
            mqttConfiguration.eventHandler,
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
            eventHandler = mqttConfiguration.eventHandler,
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
                socketFactory = mqttConfiguration.socketFactory,
                mqttInterceptorList = mqttConfiguration.mqttInterceptorList.map { it.mapToPahoInterceptor() },
                persistenceOptions = mqttConfiguration.persistenceOptions,
                inactivityTimeoutSeconds = experimentConfigs.inactivityTimeoutSeconds,
                policyResetTimeSeconds = experimentConfigs.policyResetTimeSeconds,
                isMqttVersion4Enabled = experimentConfigs.isMqttVersion4Enabled
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
        this.incomingMsgController =
            IncomingMsgControllerImpl(
                mqttUtils,
                publishSubject,
                mqttPersistence,
                logger,
                mqttConfiguration.eventHandler
            )
        networkHandler.init()
        appStateManager.addAppStateListener(object: AppStateChangeListener {
            override fun onAppStateChange(appState: AppState) {
                if(appState == AppState.FOREGROUND) {
                    onForeground()
                } else {
                    onBackground()
                }
            }
        })
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
        mqttConfiguration.eventHandler.onEvent(MqttReconnectEvent())
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

    //This runs on Mqtt thread
    override fun sendMessage(mqttPacket: MqttSendPacket) {

        if (!isConnected()) {
            connectMqtt()
        }

        try {
            logger.d(TAG, "Publishing mqtt packet on ${mqttPacket.topic} with qos ${mqttPacket.qos}")
            with(mqttPacket) {
                mqttConfiguration.eventHandler.onEvent(MqttMessageSendEvent(topic, qos, message.size))
            }
            mqttConnection.publish(mqttPacket, mqttPacket.qos, mqttPacket.topic)
        } catch (e: MqttPersistenceException) {
            with(mqttPacket) {
                mqttConfiguration.eventHandler.onEvent(
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
                mqttConfiguration.eventHandler.onEvent(
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
                mqttConfiguration.eventHandler.onEvent(
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
            mqttPacket.topic
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
            logger.e(TAG,"Remote Service dead", e)
            return false
        }

        return true
    }

    override fun receive(listener: MessageListener) {
        compositeDisposable.add(publishSubject.subscribe {
            listener.onMessageReceived(
                MqttMessage(it.topic, com.gojek.courier.Message.Bytes(it.message))
            )
        })
    }

    //This runs on Mqtt thread
    override fun connectMqtt() {
        val startTime = clock.nanoTime()
        try {
            logger.d(TAG, "Sending onConnectAttempt event")
            if(!isInitialised) {
                logger.d(TAG, "Mqtt Client not initialised")
                mqttConfiguration.eventHandler.onEvent(
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
            mqttConfiguration.eventHandler.onEvent(
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
            mqttConfiguration.eventHandler.onEvent(
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

    //This runs on Mqtt thread
    override fun disconnectMqtt(clearState: Boolean) {
        mqttConfiguration.eventHandler.onEvent(MqttDisconnectEvent())
        mqttConnection.disconnect()
        if (clearState) {
            mqttConnection.shutDown()
            subscriptionStore.clear()
            mqttPersistence.clearAll()
        }
    }

    //This runs on Mqtt thread
    override fun handleMqttException(exception: Exception?, reconnect: Boolean) {
        mqttConnection.handleException(exception, reconnect)
    }

    //This runs on Mqtt thread
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

    //This runs on Mqtt thread
    override fun isConnecting(): Boolean {
        return mqttConnection.isConnecting()
    }

    //This runs on Mqtt thread
    override fun checkActivity() {
        mqttConnection.checkActivity()
    }

    // This can be invoked on any thread
    override fun scheduleNextActivityCheck() {
        runnableScheduler.scheduleNextActivityCheck()
    }

    //This runs on Mqtt thread
    override fun subscribeMqtt(topicMap: Map<String, QoS>) {
        if (mqttConnection.isConnected()) {
            mqttConnection.subscribe(topicMap)
        }
    }

    //This runs on Mqtt thread
    override fun unsubscribeMqtt(topics: Set<String>) {
        if (mqttConnection.isConnected()) {
            mqttConnection.unsubscribe(topics)
        }
    }

    //This runs on Mqtt thread
    override fun resetParams() {
        mqttConnection.resetParams()
    }

    //This runs on Mqtt thread
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

    fun onForeground() {
        if (experimentConfigs.shouldConnectOnForeground) {
            runnableScheduler.connectMqtt()
        }
    }

    fun onBackground() {
        if (experimentConfigs.shouldConnectOnBackground) {
            runnableScheduler.connectMqtt()
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
            connectOptions.copy(
                keepAlive = keepAliveProvider.getKeepAlive(connectOptions),
                clientId = connectOptions.clientId + ":adaptive",
                isCleanSession = true
            )
        } else {
            connectOptions.copy(
                keepAlive = keepAliveProvider.getKeepAlive(connectOptions)
            )
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
                    scheme = hostFallbackPolicy!!.getServerUri().scheme,
                )
            )
        }
        return mqttConnectOptions
    }

    inner class MqttMessageReceiverListener:
        IMessageReceiveListener {
        override fun messageArrived(topic: String, byteArray: ByteArray): Boolean {
            try {
                mqttConfiguration.eventHandler.onEvent(MqttMessageReceiveEvent(topic, byteArray.size))
                val bytes = mqttUtils.uncompressByteArray(byteArray)!!
                val messageBody = String(bytes, StandardCharsets.UTF_8)
                val logMsg = "messageArrived called for message code : "
                logger.i(TAG, logMsg + messageBody)
                val mqttPacket =
                    MqttReceivePacket(
                        bytes,
                        0,
                        System.currentTimeMillis(),
                        topic
                    )
                mqttPersistence.addReceivedMessage(mqttPacket)
                triggerHandleMessage()
            } catch (e: IllegalStateException){
                mqttConfiguration.eventHandler.onEvent(
                    MqttMessageReceiveErrorEvent(topic, byteArray.size, e.toCourierException())
                )
                logger.e(TAG, "Exception when msg arrived : ", e)
                runnableScheduler.disconnectMqtt(true)
                return false
            } catch (e: Throwable) {
                mqttConfiguration.eventHandler.onEvent(
                    MqttMessageReceiveErrorEvent(topic, byteArray.size, e.toCourierException())
                )
                logger.e(TAG, "Exception when msg arrived : ", e)
            }
            return true
        }
    }

    inner class MqttMessageSendListener:
        IMessageSendListener {
        override fun onSuccess(packet: MqttSendPacket) {
            with(packet) {
                mqttConfiguration.eventHandler.onEvent(
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

        override fun notifyWrittenOnSocket(packet: MqttSendPacket) { }
    }

    companion object {
        const val TAG = "AndroidMqttClient"
    }
}