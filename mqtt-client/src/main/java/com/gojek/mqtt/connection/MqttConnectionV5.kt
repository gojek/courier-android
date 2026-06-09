package com.gojek.mqtt.connection

import android.content.Context
import com.gojek.courier.QoS
import com.gojek.courier.QoS.ONE_WITHOUT_PERSISTENCE_AND_NO_RETRY
import com.gojek.courier.QoS.ONE_WITHOUT_PERSISTENCE_AND_RETRY
import com.gojek.courier.extensions.fromNanosToMillis
import com.gojek.courier.logging.ILogger
import com.gojek.courier.utils.Clock
import com.gojek.keepalive.KeepAliveFailureHandler
import com.gojek.mqtt.client.IMessageReceiveListener
import com.gojek.mqtt.client.config.PersistenceOptions.PahoPersistenceOptions
import com.gojek.mqtt.client.mapToPahoV5Interceptor
import com.gojek.mqtt.client.model.MqttSendPacket
import com.gojek.mqtt.connection.config.v3.ConnectionConfig
import com.gojek.mqtt.event.PahoEventHandlerV5
import com.gojek.mqtt.exception.handler.v5.MqttExceptionHandler
import com.gojek.mqtt.exception.handler.v5.impl.MqttExceptionHandlerImpl
import com.gojek.mqtt.logging.PahoLoggerV5
import com.gojek.mqtt.model.ServerUri
import com.gojek.mqtt.network.NetworkHandler
import com.gojek.mqtt.persistence.impl.PahoPersistenceV5
import com.gojek.mqtt.pingsender.PahoV5TimerPingSender
import com.gojek.mqtt.policies.connectretrytime.IConnectRetryTimePolicy
import com.gojek.mqtt.policies.connecttimeout.IConnectTimeoutPolicy
import com.gojek.mqtt.policies.hostfallback.IHostFallbackPolicy
import com.gojek.mqtt.policies.subscriptionretry.ISubscriptionRetryPolicy
import com.gojek.mqtt.scheduler.IRunnableScheduler
import com.gojek.mqtt.send.listener.IMessageSendListener
import com.gojek.mqtt.subscription.SubscriptionStore
import com.gojek.mqtt.utils.NetworkUtils
import com.gojek.mqtt.wakelock.WakeLockProvider
import org.eclipse.paho.mqttv5.client.DisconnectedBufferOptions
import org.eclipse.paho.mqttv5.client.IExperimentsConfig
import org.eclipse.paho.mqttv5.client.IMqttActionListenerNew
import org.eclipse.paho.mqttv5.client.IMqttToken
import org.eclipse.paho.mqttv5.client.MqttActionListener
import org.eclipse.paho.mqttv5.client.MqttAsyncClient
import org.eclipse.paho.mqttv5.client.MqttCallback
import org.eclipse.paho.mqttv5.client.MqttClientException.REASON_CODE_INVALID_SUBSCRIPTION
import org.eclipse.paho.mqttv5.client.MqttClientException.REASON_CODE_UNEXPECTED_ERROR
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence
import org.eclipse.paho.mqttv5.common.MqttException
import org.eclipse.paho.mqttv5.common.MqttMessage
import org.eclipse.paho.mqttv5.common.MqttSecurityException
import org.eclipse.paho.mqttv5.common.packet.MqttProperties
import org.eclipse.paho.mqttv5.common.packet.SubscribeFlags
import org.eclipse.paho.mqttv5.common.packet.UserProperty

internal class MqttConnectionV5(
    private val context: Context,
    private val connectionConfig: ConnectionConfig,
    private val runnableScheduler: IRunnableScheduler,
    private val networkUtils: NetworkUtils,
    private val wakeLockProvider: WakeLockProvider,
    private val messageSendListener: IMessageSendListener,
    private val pahoPersistence: PahoPersistenceV5,
    private val networkHandler: NetworkHandler,
    private val keepAliveFailureHandler: KeepAliveFailureHandler,
    private val clock: Clock,
    private val subscriptionStore: SubscriptionStore
) : IMqttConnection {
    private var forceDisconnect = false

    @Volatile
    private var pushReConnect = false

    @Volatile
    private var fastReconnect: Short = 0

    private var options: MqttConnectionOptions? = null

    private var mqtt: MqttAsyncClient? = null

    @Volatile
    private var updatePolicyParams = false

    private val connectRetryTimePolicy: IConnectRetryTimePolicy
    private val connectTimeoutPolicy: IConnectTimeoutPolicy
    private lateinit var hostFallbackPolicy: IHostFallbackPolicy
    private val subscriptionPolicy: ISubscriptionRetryPolicy
    private val unsubscriptionPolicy: ISubscriptionRetryPolicy
    private val logger: ILogger
    private val mqttExceptionHandler: MqttExceptionHandler

    private var serverUri: ServerUri? = null

    private var connectStartTime: Long = clock.nanoTime()
    private var connectSuccessTime: Long = clock.nanoTime()

    init {
        this.connectRetryTimePolicy = connectionConfig.connectRetryTimePolicy
        this.connectTimeoutPolicy = connectionConfig.connectTimeoutPolicy
        this.subscriptionPolicy = connectionConfig.subscriptionRetryPolicy
        this.unsubscriptionPolicy = connectionConfig.unsubscriptionRetryPolicy
        this.logger = connectionConfig.logger
        this.mqttExceptionHandler =
            MqttExceptionHandlerImpl(runnableScheduler, connectRetryTimePolicy, logger)
    }

    override fun connect(
        mqttConnectOptions: com.gojek.mqtt.model.MqttConnectOptions,
        messageReceiveListener: IMessageReceiveListener,
        hostFallbackPolicy: IHostFallbackPolicy,
        subscriptionTopicMap: Map<String, QoS>
    ) {
        try {
            val connectOptions = mqttConnectOptions
            this.hostFallbackPolicy = hostFallbackPolicy
            if (forceDisconnect) {
                logger.d(TAG, "Force disconnect is in progress")
                connectionConfig.connectionEventHandler.onMqttConnectDiscarded(
                    "Force Disconnect in progress"
                )
                return
            }
            if (updatePolicyParams && !(isConnected() || isConnecting() || isDisconnecting())) {
                connectTimeoutPolicy.updateParams(true)
                updatePolicyParams = false
            }

            val clientId: String = connectOptions.clientId
            val username: String = connectOptions.username
            serverUri = getServerUri()
            logger.d(TAG, "clientId : $clientId, username: $username,  serverUri $serverUri")
            if (mqtt == null) {
                mqtt = getMqttAsyncClient(clientId, serverUri.toString())
                mqtt!!.setCallback(getMqttCallback(messageReceiveListener))
            }
            if (isConnected()) {
                logger.d(TAG, "Client already connected!!!")
                connectionConfig.connectionEventHandler.onMqttConnectDiscarded(
                    "Client already connected"
                )
                return
            }
            if (isDisconnecting()) {
                logger.d(TAG, "Client is disconnecting!!!")
                connectionConfig.connectionEventHandler.onMqttConnectDiscarded(
                    "Client disconnecting"
                )
                return
            }
            if (isConnecting()) {
                logger.d(TAG, "Client is already connecting!!!")
                connectionConfig.connectionEventHandler.onMqttConnectDiscarded(
                    "Client connecting"
                )
                return
            }

            wakeLockProvider.acquireWakeLock(connectionConfig.wakeLockTimeout)
            mqtt!!.clientId = clientId
            mqtt!!.setServerURI(serverUri.toString())

            if (options == null) {
                options = MqttConnectionOptions()
            }
            options!!.apply {
                setUserName(connectOptions.username)
                setPassword(connectOptions.password.toByteArray())
                setCleanStart(connectOptions.isCleanSession)
                setKeepAliveInterval(connectOptions.keepAlive.timeSeconds)
                setKeepAliveIntervalServer(connectOptions.keepAlive.timeSeconds)
                setReadTimeout(connectOptions.readTimeoutSecs)
                setConnectionTimeout(connectTimeoutPolicy.getConnectTimeOut())
                setHandshakeTimeout(connectTimeoutPolicy.getHandshakeTimeOut())
                setUserProperties(getUserPropertyList(connectOptions.userPropertiesMap))
                setSocketFactory(mqttConnectOptions.socketFactory)
                setSslSocketFactory(mqttConnectOptions.sslSocketFactory)
                setX509TrustManager(mqttConnectOptions.x509TrustManager)
                setConnectionSpec(mqttConnectOptions.connectionSpec.toV5ConnectionSpec())
                setAlpnProtocolList(mqttConnectOptions.protocols.map { it.toV5Protocol() })
            }

            mqttConnectOptions.will?.apply {
                val willMessage = MqttMessage(
                    message.toByteArray(),
                    qos.value,
                    retained,
                    MqttProperties()
                )
                options!!.setWill(topic, willMessage)
            }

            logger.d(TAG, "MQTT connecting on : " + mqtt!!.serverURI)
            updatePolicyParams = true
            connectStartTime = clock.nanoTime()
            connectionConfig.connectionEventHandler.onMqttConnectAttempt(
                connectOptions.keepAlive.isOptimal,
                serverUri
            )
            mqtt!!.connect(options, null, getConnectListener())
            runnableScheduler.scheduleNextActivityCheck()
        } catch (e: MqttSecurityException) {
            logger.e(TAG, "mqtt security exception while connecting $e")
            connectionConfig.connectionEventHandler.onMqttConnectFailure(
                e,
                serverUri,
                timeTakenMillis = (clock.nanoTime() - connectStartTime).fromNanosToMillis()
            )
            runnableScheduler.scheduleMqttHandleExceptionRunnable(e, false)
            wakeLockProvider.releaseWakeLock()
        } catch (e: MqttException) {
            logger.e(TAG, "Connect exception : ${e.reasonCode}")
            connectionConfig.connectionEventHandler.onMqttConnectFailure(
                e,
                serverUri,
                timeTakenMillis = (clock.nanoTime() - connectStartTime).fromNanosToMillis()
            )
            runnableScheduler.scheduleMqttHandleExceptionRunnable(e, true)
            wakeLockProvider.releaseWakeLock()
        } catch (e: Exception) {
            logger.e(TAG, "Connect exception : ${e.message}")
            connectionConfig.connectionEventHandler.onMqttConnectFailure(
                e,
                serverUri,
                timeTakenMillis = (clock.nanoTime() - connectStartTime).fromNanosToMillis()
            )
            val mqttException = MqttException(REASON_CODE_UNEXPECTED_ERROR.toInt(), e)
            runnableScheduler.scheduleMqttHandleExceptionRunnable(mqttException, true)
            wakeLockProvider.releaseWakeLock()
        }
    }

    override fun publish(mqttPacket: MqttSendPacket) {
        logger.d(TAG, "Current inflight msg count : " + mqtt!!.inflightMessages)

        mqtt!!.publishWithNewType(
            mqttPacket.topic,
            mqttPacket.message,
            mqttPacket.qos,
            mqttPacket.type,
            false,
            mqttPacket,
            object : IMqttActionListenerNew {
                override fun onSuccess(arg0: IMqttToken) {
                    logger.d(TAG, "Message successfully sent for message id : " + arg0.messageId)
                    val packet = arg0.userContext as MqttSendPacket
                    messageSendListener.onSuccess(packet)
                }

                override fun onFailure(arg0: IMqttToken, arg1: Throwable) {
                    logger.e(
                        TAG,
                        "Message delivery failed for : " + arg0.messageId +
                            ", exception : " + arg1.message
                    )
                    messageSendListener.onFailure(arg0.userContext as MqttSendPacket, arg1)
                }

                override fun notifyWrittenOnSocket(token: IMqttToken) {
                    val packet = token.userContext as MqttSendPacket
                    messageSendListener.notifyWrittenOnSocket(packet)
                }
            }
        )
    }

    override fun handleException(exception: Exception?, reconnect: Boolean) {
        if (exception == null || exception !is MqttException) {
            return
        }
        mqttExceptionHandler.handleException(exception, reconnect)
    }

    override fun isConnected(): Boolean {
        return mqtt != null && mqtt!!.isConnected
    }

    override fun isConnecting(): Boolean {
        return mqtt != null && mqtt!!.isConnecting
    }

    override fun isDisconnecting(): Boolean {
        return mqtt != null && mqtt!!.isDisconnecting
    }

    override fun isDisconnected(): Boolean {
        return mqtt != null && mqtt!!.isDisconnected
    }

    override fun isForceDisconnect(): Boolean {
        return forceDisconnect
    }

    override fun disconnect() {
        try {
            if (mqtt != null) {
                if (mqtt!!.isDisconnecting || mqtt!!.isDisconnected) {
                    logger.d(TAG, "not connected but disconnecting")
                    return
                }
                forceDisconnect = true
                connectionConfig.connectionEventHandler.onMqttDisconnectStart()
                mqtt!!.disconnectForcibly(
                    connectionConfig.quiesceTimeout.toLong(),
                    connectionConfig.disconnectTimeout.toLong(),
                    false
                )
            }
        } catch (e: java.lang.Exception) {
            logger.e(TAG, "exception while disconnecting mqtt", e)
        } finally {
            handleDisconnect()
            connectionConfig.connectionEventHandler.onMqttDisconnectComplete()
        }
    }

    private fun getUserPropertyList(userPropertiesMap: Map<String, String>): List<UserProperty> {
        val userProperties = mutableListOf<UserProperty>()
        userPropertiesMap.entries.forEach { entry ->
            userProperties.add(UserProperty(entry.key, entry.value))
        }
        return userProperties
    }

    private fun handleDisconnect() {
        resetConnectionVariables()
    }

    override fun shutDown() {
        try {
            if (mqtt != null) {
                mqtt!!.close()
            }
        } catch (e: java.lang.Exception) {
            logger.e(TAG, "exception while closing mqtt connection", e)
        }
        mqtt = null
        options = null
    }

    private fun resetConnectionVariables() {
        forceDisconnect = false
        updatePolicyParams = false
        connectTimeoutPolicy.resetParams()
    }

    override fun getServerURI(): String? {
        return if (mqtt == null) null else mqtt!!.serverURI
    }

    private fun getServerUri(): ServerUri {
        return hostFallbackPolicy.getServerUri()
    }

    override fun checkActivity() {
        if (mqtt != null) {
            mqtt!!.checkActivity()
        }
    }

    override fun resetParams() {
        connectRetryTimePolicy.resetParams()
    }

    private fun getMqttAsyncClient(clientId: String, serverUri: String): MqttAsyncClient {
        val persistence = if (connectionConfig.shouldUseMemoryPersistence) {
            MemoryPersistence()
        } else {
            pahoPersistence
        }
        val mqttAsyncClient = MqttAsyncClient(
            serverUri,
            clientId,
            com.gojek.mqtt.model.MqttVersion.VERSION_5.protocolLevel.toString(),
            persistence,
            connectionConfig.maxInflightMessages,
            PahoV5TimerPingSender(connectionConfig.logger),
            PahoLoggerV5(connectionConfig.logger),
            PahoEventHandlerV5(connectionConfig.connectionEventHandler),
            getPahoExperimentsConfig(),
            connectionConfig.mqttInterceptorList.map { mapToPahoV5Interceptor(it) }
        )
        val bufferOptions = DisconnectedBufferOptions()
        with(connectionConfig.persistenceOptions as PahoPersistenceOptions) {
            bufferOptions.isBufferEnabled = true
            bufferOptions.isPersistBuffer = true
            bufferOptions.bufferSize = bufferCapacity
            bufferOptions.isDeleteOldestMessages = isDeleteOldestMessages
        }
        mqttAsyncClient.setBufferOpts(bufferOptions)
        return mqttAsyncClient
    }

    private fun getConnectListener(): MqttActionListener {
        return object : MqttActionListener {
            override fun onSuccess(iMqttToken: IMqttToken) {
                try {
                    pushReConnect = false
                    fastReconnect = 0
                    connectSuccessTime = clock.nanoTime()
                    runnableScheduler.scheduleResetParams(
                        connectionConfig.policyResetTimeSeconds * 1000L
                    )
                    connectionConfig.connectionEventHandler.onMqttConnectSuccess(
                        serverUri = serverUri,
                        timeTakenMillis = (
                            connectSuccessTime - connectStartTime
                            ).fromNanosToMillis()
                    )
                    runnableScheduler.scheduleSubscribe(
                        0,
                        subscriptionStore.getSubscribeTopics()
                    )
                    runnableScheduler.scheduleUnsubscribe(
                        0,
                        subscriptionStore.getUnsubscribeTopics(options!!.isCleanStart)
                    )
                } finally {
                    wakeLockProvider.releaseWakeLock()
                }
            }

            override fun onFailure(iMqttToken: IMqttToken, throwable: Throwable) {
                try {
                    if (throwable is MqttException) {
                        runnableScheduler.scheduleMqttHandleExceptionRunnable(
                            e = throwable,
                            reconnect = true
                        )
                    }
                    hostFallbackPolicy.onConnectFailure(throwable)
                    connectionConfig.connectionEventHandler.onMqttConnectFailure(
                        throwable = throwable,
                        serverUri = serverUri,
                        timeTakenMillis = (clock.nanoTime() - connectStartTime).fromNanosToMillis()
                    )
                } catch (e: java.lang.Exception) {
                    logger.e(TAG, "Exception in connect failure callback", e)
                } finally {
                    wakeLockProvider.releaseWakeLock()
                }
            }
        }
    }

    override fun subscribe(topicMap: Map<String, QoS>) {
        if (topicMap.isNotEmpty()) {
            val topicArray: Array<String> = topicMap.keys.toTypedArray()
            val qosArray = IntArray(topicMap.size)
            val subscribeFlagList = ArrayList<SubscribeFlags>(topicMap.size)
            for ((index, qos) in topicMap.values.withIndex()) {
                if (qos == ONE_WITHOUT_PERSISTENCE_AND_NO_RETRY || qos == ONE_WITHOUT_PERSISTENCE_AND_RETRY) {
                    qosArray[index] = 1
                } else {
                    qosArray[index] = qos.value
                }
            }
            for ((index, qos) in topicMap.values.withIndex()) {
                when (qos) {
                    ONE_WITHOUT_PERSISTENCE_AND_NO_RETRY -> {
                        subscribeFlagList.add(index, SubscribeFlags(false, false))
                    }
                    ONE_WITHOUT_PERSISTENCE_AND_RETRY -> {
                        subscribeFlagList.add(index, SubscribeFlags(false, true))
                    }
                    else -> {
                        subscribeFlagList.add(index, SubscribeFlags(true, true))
                    }
                }
            }
            val subscribeStartTime = clock.nanoTime()
            try {
                logger.d(TAG, "Subscribing to topics: ${topicMap.keys}")
                connectionConfig.connectionEventHandler.onMqttSubscribeAttempt(topicMap)
                mqtt!!.subscribeWithPersistableRetryableFlags(
                    topicArray,
                    qosArray,
                    subscribeFlagList,
                    MqttV5Context(subscribeStartTime),
                    getSubscribeListener(topicMap)
                )
            } catch (mqttException: MqttException) {
                connectionConfig.connectionEventHandler.onMqttSubscribeFailure(
                    topics = topicMap,
                    throwable = mqttException,
                    timeTakenMillis = (clock.nanoTime() - subscribeStartTime).fromNanosToMillis()
                )
                runnableScheduler.scheduleMqttHandleExceptionRunnable(mqttException, true)
            } catch (illegalArgumentException: IllegalArgumentException) {
                connectionConfig.connectionEventHandler.onMqttSubscribeFailure(
                    topics = topicMap,
                    throwable = MqttException(
                        REASON_CODE_INVALID_SUBSCRIPTION.toInt(),
                        illegalArgumentException
                    ),
                    timeTakenMillis = (clock.nanoTime() - subscribeStartTime).fromNanosToMillis()
                )
                subscriptionStore.getListener().onInvalidTopicsSubscribeFailure(topicMap)
            }
        }
    }

    override fun unsubscribe(topics: Set<String>) {
        if (topics.isNotEmpty()) {
            val unsubscribeStartTime = clock.nanoTime()
            try {
                logger.d(TAG, "Unsubscribing to topics: $topics")
                connectionConfig.connectionEventHandler.onMqttUnsubscribeAttempt(topics)
                mqtt!!.unsubscribe(
                    topics.toTypedArray(),
                    MqttV5Context(unsubscribeStartTime),
                    getUnsubscribeListener(topics),
                    MqttProperties()
                )
            } catch (mqttException: MqttException) {
                connectionConfig.connectionEventHandler.onMqttUnsubscribeFailure(
                    topics = topics,
                    throwable = mqttException,
                    timeTakenMillis = (clock.nanoTime() - unsubscribeStartTime).fromNanosToMillis()
                )
                runnableScheduler.scheduleMqttHandleExceptionRunnable(mqttException, true)
            } catch (illegalArgumentException: IllegalArgumentException) {
                connectionConfig.connectionEventHandler.onMqttUnsubscribeFailure(
                    topics = topics,
                    throwable = MqttException(
                        REASON_CODE_INVALID_SUBSCRIPTION.toInt(),
                        illegalArgumentException
                    ),
                    timeTakenMillis = (clock.nanoTime() - unsubscribeStartTime).fromNanosToMillis()
                )
                subscriptionStore.getListener().onInvalidTopicsUnsubscribeFailure(topics)
            }
        }
    }

    private fun getSubscribeListener(topicMap: Map<String, QoS>): MqttActionListener {
        return object : MqttActionListener {
            override fun onSuccess(iMqttToken: IMqttToken) {
                logger.d(TAG, "Subscribe successful. Connect Complete")
                val context = iMqttToken.userContext as MqttV5Context
                val successTopicMap = mutableMapOf<String, QoS>()
                val failTopicMap = mutableMapOf<String, QoS>()
                val reasonCodes = iMqttToken.reasonCodes
                iMqttToken.topics.forEachIndexed { index, topic ->
                    if ((reasonCodes?.getOrNull(index) ?: 0) >= 128) {
                        failTopicMap[topic] = topicMap[topic]!!
                    } else {
                        successTopicMap[topic] = topicMap[topic]!!
                    }
                }

                if (successTopicMap.isNotEmpty()) {
                    connectionConfig.connectionEventHandler.onMqttSubscribeSuccess(
                        topics = successTopicMap,
                        timeTakenMillis = (clock.nanoTime() - context.startTime).fromNanosToMillis()
                    )
                }

                if (failTopicMap.isNotEmpty()) {
                    connectionConfig.connectionEventHandler.onMqttSubscribeFailure(
                        topics = failTopicMap,
                        timeTakenMillis = (clock.nanoTime() - context.startTime).fromNanosToMillis(),
                        throwable = MqttException(REASON_CODE_INVALID_SUBSCRIPTION.toInt())
                    )
                }

                subscriptionStore.getListener().onTopicsSubscribed(successTopicMap)
                subscriptionStore.getListener().onInvalidTopicsSubscribeFailure(failTopicMap)
                subscriptionPolicy.resetParams()
            }

            override fun onFailure(iMqttToken: IMqttToken, throwable: Throwable) {
                if (subscriptionPolicy.shouldRetry()) {
                    logger.e(TAG, "Subscribe unsuccessful. Will retry again")
                    runnableScheduler.scheduleSubscribe(10, topicMap)
                } else {
                    logger.e(TAG, "Subscribe unsuccessful. Will reconnect again")
                    val context = iMqttToken.userContext as MqttV5Context
                    connectionConfig.connectionEventHandler.onMqttSubscribeFailure(
                        topics = topicMap,
                        throwable = throwable,
                        timeTakenMillis = (clock.nanoTime() - context.startTime).fromNanosToMillis()
                    )
                    runnableScheduler.disconnectMqtt(true)
                }
            }
        }
    }

    private fun getUnsubscribeListener(topics: Set<String>): MqttActionListener {
        return object : MqttActionListener {
            override fun onSuccess(iMqttToken: IMqttToken) {
                logger.d(TAG, "Unsubscribe successful")
                val context = iMqttToken.userContext as MqttV5Context
                connectionConfig.connectionEventHandler.onMqttUnsubscribeSuccess(
                    topics = topics,
                    timeTakenMillis = (clock.nanoTime() - context.startTime).fromNanosToMillis()
                )
                unsubscriptionPolicy.resetParams()
                subscriptionStore.getListener().onTopicsUnsubscribed(topics)
            }

            override fun onFailure(iMqttToken: IMqttToken, throwable: Throwable) {
                if (unsubscriptionPolicy.shouldRetry()) {
                    logger.e(TAG, "Unsubscribe unsuccessful. Will retry again")
                    runnableScheduler.scheduleUnsubscribe(10, topics)
                } else {
                    logger.e(TAG, "Unsubscribe unsuccessful. Will reconnect again")
                    val context = iMqttToken.userContext as MqttV5Context
                    connectionConfig.connectionEventHandler.onMqttUnsubscribeFailure(
                        topics = topics,
                        throwable = throwable,
                        timeTakenMillis = (clock.nanoTime() - context.startTime).fromNanosToMillis()
                    )
                    runnableScheduler.disconnectMqtt(true)
                }
            }
        }
    }

    private fun getMqttCallback(messageReceiveListener: IMessageReceiveListener): MqttCallback {
        return object : MqttCallback {
            override fun disconnected(disconnectResponse: MqttDisconnectResponse) {
                val throwable: Throwable = disconnectResponse.exception
                    ?: MqttException(org.eclipse.paho.mqttv5.client.MqttClientException.REASON_CODE_CONNECTION_LOST.toInt())
                handleConnectionLost(throwable)
            }

            override fun mqttErrorOccurred(exception: MqttException) {
                logger.w(TAG, "Mqtt error occurred : ${exception.message}")
            }

            @Throws(java.lang.Exception::class)
            override fun messageArrived(topic: String, message: MqttMessage): Boolean {
                return messageReceiveListener.messageArrived(topic, message.payload)
            }

            override fun deliveryComplete(token: IMqttToken) {
                // nothing needs to be done here as success will get called eventually
            }

            override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                // nothing needs to be done here
            }

            override fun authPacketArrived(reasonCode: Int, properties: MqttProperties?) {
                // nothing needs to be done here
            }

            override fun fastReconnect() {
                // nothing needs to be done here
            }
        }
    }

    private fun handleConnectionLost(throwable: Throwable) {
        logger.w(TAG, "Connection Lost : ${throwable.message}")
        if (networkUtils.isConnected(context)) {
            keepAliveFailureHandler.handleKeepAliveFailure()
        }
        val connRetryTimeSecs = connectRetryTimePolicy.getConnRetryTimeSecs()
        runnableScheduler.connectMqtt(connRetryTimeSecs * 1000L)
        connectionConfig.connectionEventHandler.onMqttConnectionLost(
            throwable = throwable,
            serverUri = serverUri,
            nextRetryTimeSecs = connRetryTimeSecs,
            sessionTimeMillis = (clock.nanoTime() - connectSuccessTime).fromNanosToMillis()
        )
    }

    private fun getPahoExperimentsConfig(): IExperimentsConfig {
        return object : IExperimentsConfig {
            override fun inactivityTimeoutSecs(): Int {
                return connectionConfig.inactivityTimeoutSeconds
            }

            override fun connectPacketTimeoutSecs(): Int {
                return connectionConfig.connectPacketTimeoutSeconds
            }

            override fun useNewSSLFlow(): Boolean {
                return connectionConfig.shouldUseNewSSLFlow
            }
        }
    }

    companion object {
        const val TAG = "MqttConnectionV5"
    }
}

private data class MqttV5Context(val startTime: Long)
