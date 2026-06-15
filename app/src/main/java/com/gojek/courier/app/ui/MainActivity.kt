package com.gojek.courier.app.ui

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.gojek.chuckmqtt.external.MqttChuckConfig
import com.gojek.chuckmqtt.external.MqttChuckInterceptor
import com.gojek.chuckmqtt.external.Period
import com.gojek.courier.Courier
import com.gojek.courier.QoS
import com.gojek.courier.QoS.ZERO
import com.gojek.courier.app.data.network.CourierService
import com.gojek.courier.app.data.network.model.Message
import com.gojek.courier.app.databinding.ActivityMainBinding
import com.gojek.courier.callback.SendMessageCallback
import com.gojek.courier.logging.ILogger
import com.gojek.courier.messageadapter.gson.GsonMessageAdapterFactory
import com.gojek.courier.messageadapter.text.TextMessageAdapterFactory
import com.gojek.courier.streamadapter.rxjava2.RxJava2StreamAdapterFactory
import com.gojek.mqtt.auth.Authenticator
import com.gojek.mqtt.client.MqttClient
import com.gojek.mqtt.client.config.ExperimentConfigs
import com.gojek.mqtt.client.config.PersistenceOptions.PahoPersistenceOptions
import com.gojek.mqtt.client.config.v3.MqttV3Configuration
import com.gojek.mqtt.client.factory.MqttClientFactory
import com.gojek.mqtt.event.EventHandler
import com.gojek.mqtt.event.MqttEvent
import com.gojek.mqtt.model.AdaptiveKeepAliveConfig
import com.gojek.mqtt.model.KeepAlive
import com.gojek.mqtt.model.MqttConnectOptions
import com.gojek.mqtt.model.ServerUri
import com.gojek.mqtt.model.Will
import com.gojek.workmanager.pingsender.WorkManagerPingSenderConfig
import com.gojek.workmanager.pingsender.WorkPingSenderFactory
import timber.log.Timber
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var mqttClient: MqttClient
    private lateinit var courierService: CourierService

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initialiseCourier()
        binding.connect.setOnClickListener {
            var clientId = binding.clientId.text.toString()
            if(clientId.isEmpty()) {
                clientId = UUID.randomUUID().toString()
            }
            var username = binding.username.text.toString()
            if(username.isEmpty()) {
                username = UUID.randomUUID().toString()
            }
            val password = binding.password.text.toString()
            var brokerIP = binding.brokerIP.text.toString()
            if(brokerIP.isEmpty()) {
                brokerIP = "broker.hivemq.com"
            }
            var port = 8883
            if(binding.brokerPort.text.toString().isNotEmpty()) {
                port = Integer.parseInt(binding.brokerPort.text.toString())
            }
            connectMqtt(clientId, username, password, brokerIP, port)
        }

        binding.disconnect.setOnClickListener {
            mqttClient.disconnect()
        }

        binding.send.setOnClickListener {
            courierService.publish(
                topic = binding.topic.text.toString(),
                message = Message(123, binding.message.text.toString()),
                callback = object : SendMessageCallback {
                    override fun onMessageSendTrigger() {
                        Log.d("Courier", "onMessageSendTrigger")
                    }

                    override fun onMessageWrittenOnSocket() {
                        Log.d("Courier", "onMessageWrittenOnSocket")
                    }

                    override fun onMessageSendSuccess() {
                        Log.d("Courier", "onMessageSendSuccess")
                    }

                    override fun onMessageSendFailure(error: Throwable) {
                        Log.d("Courier", "onMessageSendFailure")
                    }
                }
            )
        }

        binding.subscribe.setOnClickListener {
            val topics = binding.topic.text.toString().split(",")
            val stream = if (topics.size == 1) {
                courierService.subscribe(topic = topics[0])
            } else {
                val topicMap = mutableMapOf<String, QoS>()
                for (topic in topics) { topicMap[topic] = ZERO }
                courierService.subscribeAll(topicMap = topicMap)
            }
            stream.subscribe { Log.d("Courier", "Message received: $it") }
        }

        binding.unsubscribe.setOnClickListener {
            courierService.unsubscribe(topic = binding.topic.text.toString())
        }
    }

    private fun connectMqtt(clientId: String, username: String, password: String, ip: String, port: Int) {

        val will = Will(
            topic = "last/will/topic",
            message = "Client disconnected unexpectedly",
            qos = QoS.ZERO,
            retained = false
        )

        val connectOptions = MqttConnectOptions.Builder()
            .serverUris(listOf(ServerUri(ip, port, if (port == 443 || port == 8883) "ssl" else "tcp")))
            .clientId(clientId)
            .userName(username)
            .password(password)
            .cleanSession(false)
            .will(will)
            .keepAlive(KeepAlive(timeSeconds = 30))
            .build()

        mqttClient.connect(connectOptions)
    }

    private fun initialiseCourier() {
        val mqttConfig = MqttV3Configuration(
            logger = getLogger(),
            authenticator = object : Authenticator {
                override fun authenticate(
                    connectOptions: MqttConnectOptions,
                    forceRefresh: Boolean
                ): MqttConnectOptions {
                    return connectOptions.newBuilder()
                        .password(binding.password.text.toString())
                        .build()
                }
            },
            mqttInterceptorList = listOf(MqttChuckInterceptor(this, MqttChuckConfig(retentionPeriod = Period.ONE_HOUR))),
            persistenceOptions = PahoPersistenceOptions(100, false),
            experimentConfigs = ExperimentConfigs(
                adaptiveKeepAliveConfig = AdaptiveKeepAliveConfig(
                    lowerBoundMinutes = 1,
                    upperBoundMinutes = 9,
                    stepMinutes = 2,
                    optimalKeepAliveResetLimit = 10,
                    pingSender = WorkPingSenderFactory.createAdaptiveMqttPingSender(applicationContext, WorkManagerPingSenderConfig())
                ),
                inactivityTimeoutSeconds = 45,
                activityCheckIntervalSeconds = 30,
                connectPacketTimeoutSeconds = 5,
                incomingMessagesTTLSecs = 60,
                incomingMessagesCleanupIntervalSecs = 10,
                maxInflightMessagesLimit = 1000,
                isMqttVersion5Enabled = true
            ),
            pingSender = WorkPingSenderFactory.createMqttPingSender(applicationContext, WorkManagerPingSenderConfig(sendForcePing = true))
        )
        mqttClient = MqttClientFactory.create(this, mqttConfig)
        mqttClient.addEventHandler(eventHandler)

        val configuration = Courier.Configuration(
            client = mqttClient,
            streamAdapterFactories = listOf(RxJava2StreamAdapterFactory()),
            messageAdapterFactories = listOf(TextMessageAdapterFactory(), GsonMessageAdapterFactory()),
            logger = getLogger()
        )
        val courier = Courier(configuration)
        courierService = courier.create()
    }

    private val eventHandler = object : EventHandler {
        override fun onEvent(mqttEvent: MqttEvent) {
            Timber.tag("Courier").d("Received event: $mqttEvent")
        }
    }

    private fun getLogger() = object : ILogger {
        override fun v(tag: String, msg: String) {
            Timber.tag("Courier").v(msg)
        }

        override fun v(tag: String, msg: String, tr: Throwable) {
            Timber.tag("Courier").v(tr, msg)
        }

        override fun d(tag: String, msg: String) {
            Timber.tag("Courier").d(msg)
        }

        override fun d(tag: String, msg: String, tr: Throwable) {
            Timber.tag("Courier").d(tr, msg)
        }

        override fun i(tag: String, msg: String) {
            Timber.tag("Courier").i(msg)
        }

        override fun i(tag: String, msg: String, tr: Throwable) {
            Timber.tag("Courier").i(tr, msg)
        }

        override fun w(tag: String, msg: String) {
            Timber.tag("Courier").w(msg)
        }

        override fun w(tag: String, msg: String, tr: Throwable) {
            Timber.tag("Courier").w(tr, msg)
        }

        override fun w(tag: String, tr: Throwable) {
            Timber.tag("Courier").d(tr)
        }

        override fun e(tag: String, msg: String) {
            Timber.tag("Courier").e(msg)
        }

        override fun e(tag: String, msg: String, tr: Throwable) {
            Timber.tag("Courier").e(tr, msg)
        }
    }
}
