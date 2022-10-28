package com.gojek.mqtt.model

import com.gojek.mqtt.model.KeepAlive.Companion.NO_KEEP_ALIVE
import com.gojek.mqtt.model.MqttVersion.VERSION_3_1_1
import javax.net.SocketFactory
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.X509TrustManager
import org.eclipse.paho.client.mqttv3.ConnectionSpec
import org.eclipse.paho.client.mqttv3.Protocol
import org.eclipse.paho.client.mqttv3.internal.platform.Platform

class MqttConnectOptions private constructor(
    builder: Builder
) {
    val serverUris: List<ServerUri> = builder.serverUris

    val keepAlive: KeepAlive = builder.keepAlive

    val clientId: String = builder.clientId

    val username: String = builder.username

    val password: String = builder.password

    val isCleanSession: Boolean = builder.isCleanSession

    val readTimeoutSecs: Int

    val version: MqttVersion = builder.version

    val userPropertiesMap: Map<String, String> = builder.userPropertiesMap

    val socketFactory: SocketFactory = builder.socketFactory

    val sslSocketFactory: SSLSocketFactory?

    val x509TrustManager: X509TrustManager?

    val connectionSpec: ConnectionSpec =
        builder.connectionSpec

    val protocols: List<Protocol> = builder.protocols

    init {
        if (connectionSpec.isTls.not()) {
            this.sslSocketFactory = null
            this.x509TrustManager = null
        } else if (builder.sslSocketFactoryOrNull != null) {
            this.sslSocketFactory = builder.sslSocketFactoryOrNull
            this.x509TrustManager = builder.x509TrustManagerOrNull!!
        } else {
            this.x509TrustManager = Platform.get().platformTrustManager()
            this.sslSocketFactory = Platform.get().newSslSocketFactory(x509TrustManager!!)
        }

        this.readTimeoutSecs = if (builder.readTimeoutSecs < builder.keepAlive.timeSeconds) {
            builder.keepAlive.timeSeconds + 60
        } else {
            builder.readTimeoutSecs
        }
    }

    fun newBuilder(): Builder = Builder(this)

    class Builder() {
        internal var serverUris: List<ServerUri> = emptyList()
        internal var keepAlive: KeepAlive = NO_KEEP_ALIVE
        internal var clientId: String = ""
        internal var username: String = ""
        internal var password: String = ""
        internal var isCleanSession: Boolean = false
        internal var readTimeoutSecs: Int = DEFAULT_READ_TIMEOUT
        internal var version: MqttVersion = VERSION_3_1_1
        internal var userPropertiesMap: Map<String, String> = emptyMap()
        internal var socketFactory: SocketFactory = SocketFactory.getDefault()
        internal var sslSocketFactoryOrNull: SSLSocketFactory? = null
        internal var x509TrustManagerOrNull: X509TrustManager? = null
        internal var connectionSpec: ConnectionSpec = DEFAULT_CONNECTION_SPECS
        internal var protocols: List<Protocol> = emptyList()

        internal constructor(mqttConnectOptions: MqttConnectOptions) : this() {
            this.serverUris = mqttConnectOptions.serverUris
            this.keepAlive = mqttConnectOptions.keepAlive
            this.clientId = mqttConnectOptions.clientId
            this.username = mqttConnectOptions.username
            this.password = mqttConnectOptions.password
            this.isCleanSession = mqttConnectOptions.isCleanSession
            this.readTimeoutSecs = mqttConnectOptions.readTimeoutSecs
            this.version = mqttConnectOptions.version
            this.userPropertiesMap = mqttConnectOptions.userPropertiesMap
            this.socketFactory = mqttConnectOptions.socketFactory
            this.sslSocketFactoryOrNull = mqttConnectOptions.sslSocketFactory
            this.x509TrustManagerOrNull = mqttConnectOptions.x509TrustManager
            this.connectionSpec = mqttConnectOptions.connectionSpec
            this.protocols = mqttConnectOptions.protocols
        }

        fun serverUris(serverUris: List<ServerUri>) = apply {
            require(serverUris.isNotEmpty()) { "serverUris cannot be empty" }
            this.serverUris = serverUris
        }

        fun keepAlive(keepAlive: KeepAlive) = apply {
            require(keepAlive.timeSeconds > 0) { "keepAlive timeSeconds must be >0" }
            this.keepAlive = keepAlive
        }

        fun clientId(clientId: String) = apply {
            require(clientId.isNotEmpty()) { "clientId cannot be empty" }
            this.clientId = clientId
        }

        fun userName(username: String) = apply {
            this.username = username
        }

        fun password(password: String) = apply {
            this.password = password
        }

        fun cleanSession(cleanSession: Boolean) = apply {
            this.isCleanSession = cleanSession
        }

        fun readTimeoutSecs(readTimeoutSecs: Int) = apply {
            require(readTimeoutSecs > 0) { "read timeout should be > 0" }
            this.readTimeoutSecs = readTimeoutSecs
        }

        fun mqttVersion(mqttVersion: MqttVersion) = apply {
            this.version = mqttVersion
        }

        fun userProperties(userProperties: Map<String, String>) = apply {
            this.userPropertiesMap = userProperties.toMap()
        }

        /**
         * Sets the socket factory used to create connections.
         *
         * If unset, the [system-wide default][SocketFactory.getDefault] socket factory will be used.
         */
        fun socketFactory(socketFactory: SocketFactory) = apply {
            require(socketFactory !is SSLSocketFactory) { "socketFactory instanceof SSLSocketFactory" }

            this.socketFactory = socketFactory
        }

        /**
         * Sets the socket factory and trust manager used to secure MQTT/Websocket connections. If unset, the
         * system defaults will be used.
         *
         * Most applications should not call this method, and instead use the system defaults. Those
         * classes include special optimizations that can be lost if the implementations are decorated.
         *
         * If necessary, you can create and configure the defaults yourself with the following code:
         *
         * ```java
         * TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
         * TrustManagerFactory.getDefaultAlgorithm());
         * trustManagerFactory.init((KeyStore) null);
         * TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
         * if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
         *     throw new IllegalStateException("Unexpected default trust managers:"
         *         + Arrays.toString(trustManagers));
         * }
         * X509TrustManager trustManager = (X509TrustManager) trustManagers[0];
         *
         * SSLContext sslContext = SSLContext.getInstance("TLS");
         * sslContext.init(null, new TrustManager[] { trustManager }, null);
         * SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
         *
         * MQTTConnectOptions client = MQTTConnectOptions.Builder()
         *     .sslSocketFactory(sslSocketFactory, trustManager)
         *     .build();
         * ```
         *
         * ## TrustManagers on Android are Weird!
         *
         * Trust managers targeting Android must also define a method that has this signature:
         *
         * ```java
         *    @SuppressWarnings("unused")
         *    public List<X509Certificate> checkServerTrusted(
         *        X509Certificate[] chain, String authType, String host) throws CertificateException {
         *    }
         * ```
         * See [android.net.http.X509TrustManagerExtensions] for more information.
         */
        fun sslSocketFactory(
            sslSocketFactory: SSLSocketFactory,
            trustManager: X509TrustManager
        ) = apply {
            this.sslSocketFactoryOrNull = sslSocketFactory
            this.x509TrustManagerOrNull = trustManager
        }

        fun connectionSpec(connectionSpec: ConnectionSpec) = apply {
            this.connectionSpec = connectionSpec
        }

        fun alpnProtocols(protocols: List<Protocol>) = apply {
            require(protocols.isNotEmpty()) { "alpn protocol list cannot be empty" }
            this.protocols = protocols
        }

        fun build(): MqttConnectOptions = MqttConnectOptions(this)
    }

    companion object {

        internal const val DEFAULT_READ_TIMEOUT = -1

        internal val DEFAULT_CONNECTION_SPECS = ConnectionSpec.MODERN_TLS
    }
}

enum class MqttVersion(internal val protocolName: String, internal val protocolLevel: Int) {
    VERSION_3_1("MQIsdp", 3), VERSION_3_1_1("MQTT", 4)
}
