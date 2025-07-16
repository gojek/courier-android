package com.gojek.mqtt.connection

import android.content.Context
import android.os.SystemClock
import com.gojek.courier.QoS
import com.gojek.courier.QoS.ONE_WITHOUT_PERSISTENCE_AND_NO_RETRY
import com.gojek.courier.QoS.ONE_WITHOUT_PERSISTENCE_AND_RETRY
import com.gojek.courier.extensions.fromNanosToMillis
import com.gojek.courier.logging.ILogger
import com.gojek.courier.utils.Clock
import com.gojek.keepalive.KeepAliveFailureHandler
import com.gojek.mqtt.client.IMessageReceiveListener
import com.gojek.mqtt.client.config.PersistenceOptions.PahoPersistenceOptions
import com.gojek.mqtt.client.model.MqttSendPacket
import com.gojek.mqtt.connection.config.v3.ConnectionConfig
import com.gojek.mqtt.event.PahoEventHandler
import com.gojek.mqtt.exception.handler.v3.MqttExceptionHandler
import com.gojek.mqtt.exception.handler.v3.impl.MqttExceptionHandlerImpl
import com.gojek.mqtt.logging.PahoLogger
import com.gojek.mqtt.model.ServerUri
import com.gojek.mqtt.network.NetworkHandler
import com.gojek.mqtt.persistence.impl.PahoPersistence
import com.gojek.mqtt.pingsender.MqttPingSender
import com.gojek.mqtt.pingsender.toPahoPingSender
import com.gojek.mqtt.policies.connectretrytime.IConnectRetryTimePolicy
import com.gojek.mqtt.policies.connecttimeout.IConnectTimeoutPolicy
import com.gojek.mqtt.policies.hostfallback.IHostFallbackPolicy
import com.gojek.mqtt.policies.subscriptionretry.ISubscriptionRetryPolicy
import com.gojek.mqtt.scheduler.IRunnableScheduler
import com.gojek.mqtt.send.listener.IMessageSendListener
import com.gojek.mqtt.subscription.SubscriptionStore
import com.gojek.mqtt.utils.NetworkUtils
import com.gojek.mqtt.wakelock.WakeLockProvider
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions
import org.eclipse.paho.client.mqttv3.IExperimentsConfig
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttActionListenerNew
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttAsyncClient
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttException.REASON_CODE_INVALID_SUBSCRIPTION
import org.eclipse.paho.client.mqttv3.MqttException.REASON_CODE_UNEXPECTED_ERROR
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.MqttSecurityException
import org.eclipse.paho.client.mqttv3.internal.wire.MqttSuback
import org.eclipse.paho.client.mqttv3.internal.wire.SubscribeFlags
import org.eclipse.paho.client.mqttv3.internal.wire.UserProperty

internal class MqttConnection(
    private val context: Context,
    private val connectionConfig: ConnectionConfig,
    private val runnableScheduler: IRunnableScheduler,
    private val networkUtils: NetworkUtils,
    private val wakeLockProvider: WakeLockProvider,
    private val messageSendListener: IMessageSendListener,
    private val pahoPersistence: PahoPersistence,
    private val networkHandler: NetworkHandler,
    private val mqttPingSender: MqttPingSender,
    private val keepAliveFailureHandler: KeepAliveFailureHandler,
    private val clock: Clock,
    private val subscriptionStore: SubscriptionStore
) : IMqttConnection {
    private var forceDisconnect = false

    @Volatile
    private var pushReConnect = false

    @Volatile
    private var fastReconnect: Short = 0

    private var options: MqttConnectOptions? = null

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
            var connectOptions = mqttConnectOptions
            this.hostFallbackPolicy = hostFallbackPolicy
            // if force disconnect is in progress don't connect
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
                logger.d(TAG, "Number of max inflight msgs allowed : " + mqtt!!.maxflightMessages)
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
            mqtt!!.serverURI = serverUri.toString()

            if (options == null) {
                options = MqttConnectOptions()
            }
            options!!.apply {
                userName = connectOptions.username
                password = connectOptions.password.toCharArray()
                isCleanSession = connectOptions.isCleanSession
                keepAliveInterval = connectOptions.keepAlive.timeSeconds
                keepAliveIntervalServer = connectOptions.keepAlive.timeSeconds
                readTimeout = connectOptions.readTimeoutSecs
                connectionTimeout = connectTimeoutPolicy.getConnectTimeOut()
                handshakeTimeout = connectTimeoutPolicy.getHandshakeTimeOut()
                protocolName = mqttConnectOptions.version.protocolName
                protocolLevel = mqttConnectOptions.version.protocolLevel
                userPropertyList = getUserPropertyList(connectOptions.userPropertiesMap)
                socketFactory = mqttConnectOptions.socketFactory
                sslSocketFactory = mqttConnectOptions.sslSocketFactory
                x509TrustManager = mqttConnectOptions.x509TrustManager
                connectionSpec = mqttConnectOptions.connectionSpec
                alpnProtocolList = mqttConnectOptions.protocols
            }

            mqttConnectOptions.will?.apply {
                options!!.setWill(
                    topic,
                    message.toByteArray(),
                    qos.value,
                    retained
                )
            }

            // Setting some connection options which we need to reset on every connect

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
        } catch (e: java.lang.Exception) // this exception cannot be thrown on connect
        {
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

    override fun publish(
        mqttPacket: MqttSendPacket
    ) {
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

                override fun onFailure(
                    arg0: IMqttToken,
                    arg1: Throwable
                ) {
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
        // defensive check
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
                /*
                 * If already disconnecting or disconnected no need to disconnect
                 */
                if (mqtt!!.isDisconnecting || mqtt!!.isDisconnected) {
                    logger.d(TAG, "not connected but disconnecting")
                    if (mqtt!!.isDisconnecting) {
                        logger.d(TAG, "already disconnecting")
                    } else if (mqtt!!.isDisconnected) {
                        logger.d(TAG, "already disconnected")
                    }
                    return
                }
                forceDisconnect = true
                /*
                 * blocking the mqtt thread, so that no other operation takes place
                 * till disconnects completes or timeout This will wait for max 1 secs
                 */
                connectionConfig.connectionEventHandler.onMqttDisconnectStart()
                mqtt!!.disconnectForcibly(
                    connectionConfig.quiesceTimeout.toLong(),
                    connectionConfig.disconnectTimeout.toLong()
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

    private fun isPasswordExpired(passwordExpiry: Long): Boolean {
        return if (passwordExpiry == -1L) {
            return false
        } else {
            SystemClock.elapsedRealtime() >= passwordExpiry
        }
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
        val mqttAsyncClient = MqttAsyncClient(
            serverUri,
            clientId,
            null,
            pahoPersistence,
            connectionConfig.maxInflightMessages,
            this.mqttPingSender.toPahoPingSender(),
            PahoLogger(connectionConfig.logger),
            PahoEventHandler(connectionConfig.connectionEventHandler),
            getPahoExperimentsConfig(),
            connectionConfig.mqttInterceptorList
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

    private fun getConnectListener(): IMqttActionListener {
        return object : IMqttActionListener {
            override fun onSuccess(iMqttToken: IMqttToken) {
                try {
                    pushReConnect = false
                    fastReconnect = 0
                    connectSuccessTime = clock.nanoTime()
                    // resetting the reconnect timer to 0 as it would have been changed in failure
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
                        subscriptionStore.getUnsubscribeTopics(options!!.isCleanSession)
                    )
                } finally {
                    wakeLockProvider.releaseWakeLock()
                }
            }

            override fun onFailure(
                iMqttToken: IMqttToken,
                throwable: Throwable
            ) {
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
                    MqttContext(subscribeStartTime),
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
                    MqttContext(unsubscribeStartTime),
                    getUnsubscribeListener(topics)
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

    private fun getSubscribeListener(topicMap: Map<String, QoS>): IMqttActionListener {
        return object : IMqttActionListener {
            override fun onSuccess(iMqttToken: IMqttToken) {
                logger.d(TAG, "Subscribe successful. Connect Complete")
                val context = iMqttToken.userContext as MqttContext
                val successTopicMap = mutableMapOf<String, QoS>()
                val failTopicMap = mutableMapOf<String, QoS>()
                iMqttToken.topics.forEachIndexed { index, topic ->
                    if (128 == (iMqttToken.response as? MqttSuback)?.grantedQos?.getOrNull(index)) {
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

            override fun onFailure(
                iMqttToken: IMqttToken,
                throwable: Throwable
            ) {
                if (subscriptionPolicy.shouldRetry()) {
                    logger.e(TAG, "Subscribe unsuccessful. Will retry again")
                    runnableScheduler.scheduleSubscribe(10, topicMap)
                } else {
                    // Reconnect
                    logger.e(TAG, "Subscribe unsuccessful. Will reconnect again")
                    val context = iMqttToken.userContext as MqttContext
                    connectionConfig.connectionEventHandler.onMqttSubscribeFailure(
                        topics = topicMap,
                        throwable = throwable,
                        timeTakenMillis = (clock.nanoTime() - context.startTime).fromNanosToMillis()
                    )
                    runnableScheduler.disconnectMqtt(reconnect = true, clearState = false)
                }
            }
        }
    }

    private fun getUnsubscribeListener(topics: Set<String>): IMqttActionListener {
        return object : IMqttActionListener {
            override fun onSuccess(iMqttToken: IMqttToken) {
                logger.d(TAG, "Unsubscribe successful")
                val context = iMqttToken.userContext as MqttContext
                connectionConfig.connectionEventHandler.onMqttUnsubscribeSuccess(
                    topics = topics,
                    timeTakenMillis = (clock.nanoTime() - context.startTime).fromNanosToMillis()
                )
                unsubscriptionPolicy.resetParams()
                subscriptionStore.getListener().onTopicsUnsubscribed(topics)
            }

            override fun onFailure(
                iMqttToken: IMqttToken,
                throwable: Throwable
            ) {
                if (unsubscriptionPolicy.shouldRetry()) {
                    logger.e(TAG, "Unsubscribe unsuccessful. Will retry again")
                    runnableScheduler.scheduleUnsubscribe(10, topics)
                } else {
                    // Reconnect
                    logger.e(TAG, "Unsubscribe unsuccessful. Will reconnect again")
                    val context = iMqttToken.userContext as MqttContext
                    connectionConfig.connectionEventHandler.onMqttUnsubscribeFailure(
                        topics = topics,
                        throwable = throwable,
                        timeTakenMillis = (clock.nanoTime() - context.startTime).fromNanosToMillis()
                    )
                    runnableScheduler.disconnectMqtt(reconnect = true, clearState = false)
                }
            }
        }
    }

    private fun getMqttCallback(messageReceiveListener: IMessageReceiveListener): MqttCallback {
        return object : MqttCallback {
            override fun connectionLost(throwable: Throwable) {
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

            @Throws(java.lang.Exception::class)
            override fun messageArrived(
                topic: String,
                mqttMessage: MqttMessage
            ): Boolean {
                return messageReceiveListener.messageArrived(topic, mqttMessage.payload)
            }

            override fun deliveryComplete(iMqttDeliveryToken: IMqttDeliveryToken) {
                // nothing needs to be done here as success will get called eventually
            }

            override fun fastReconnect() {
                // nothing needs to be done here
            }
        }
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

    private fun isSSL(): Boolean {
        if (mqtt != null) {
            val uri = mqtt!!.serverURI
            return uri != null && uri.startsWith("ssl")
        }
        return false
    }

    companion object {
        const val TAG = "MqttConnectionV2"
    }
}

private data class MqttContext(val startTime: Long)
