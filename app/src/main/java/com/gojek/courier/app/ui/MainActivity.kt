package com.gojek.courier.app.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.gojek.chuckmqtt.external.MqttChuckConfig
import com.gojek.chuckmqtt.external.MqttChuckInterceptor
import com.gojek.chuckmqtt.external.Period
import com.gojek.courier.Courier
import com.gojek.courier.Message
import com.gojek.courier.QoS
import com.gojek.courier.app.R
import com.gojek.courier.logging.ILogger
import com.gojek.courier.messageadapter.gson.GsonMessageAdapterFactory
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
import com.gojek.workmanager.pingsender.WorkManagerPingSenderConfig
import com.gojek.workmanager.pingsender.WorkPingSenderFactory
import kotlinx.android.synthetic.main.activity_main.brokerIP
import kotlinx.android.synthetic.main.activity_main.brokerPort
import kotlinx.android.synthetic.main.activity_main.clientId
import kotlinx.android.synthetic.main.activity_main.connect
import kotlinx.android.synthetic.main.activity_main.disconnect
import kotlinx.android.synthetic.main.activity_main.message
import kotlinx.android.synthetic.main.activity_main.password
import kotlinx.android.synthetic.main.activity_main.send
import kotlinx.android.synthetic.main.activity_main.subscribe
import kotlinx.android.synthetic.main.activity_main.topic
import kotlinx.android.synthetic.main.activity_main.unsubscribe
import kotlinx.android.synthetic.main.activity_main.username
import timber.log.Timber
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var mqttClient: MqttClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initialiseMqtt()
        connect.setOnClickListener {
            var clientId = clientId.text.toString()
            if(clientId.isEmpty()) {
                clientId = UUID.randomUUID().toString()
            }
            var username = username.text.toString()
            if(username.isEmpty()) {
                username = UUID.randomUUID().toString()
            }
            val password = password.text.toString()
            var brokerIP = brokerIP.text.toString()
            if(brokerIP.isEmpty()) {
                brokerIP = "broker.mqttdashboard.com"
            }
            var port = 1883
            if(brokerPort.text.toString().isNotEmpty()) {
                port = Integer.parseInt(brokerPort.text.toString())
            }
            connectMqtt(clientId, username, password, brokerIP, port)
        }

        disconnect.setOnClickListener {
            mqttClient.disconnect()
        }

        send.setOnClickListener {
            mqttClient.send(Message.Bytes(message.text.toString().toByteArray()), topic.text.toString(), QoS.ONE)
        }

        subscribe.setOnClickListener {
            mqttClient.subscribe(topic.text.toString() to QoS.ONE)
        }

        unsubscribe.setOnClickListener {
            mqttClient.unsubscribe(topic.text.toString())
        }
    }

    private fun connectMqtt(clientId: String, username: String, password: String, ip: String, port: Int) {
        val connectOptions = MqttConnectOptions(
            serverUris = listOf(ServerUri(ip, port, if (port == 443) "ssl" else "tcp")),
            clientId = clientId,
            username = username,
            keepAlive = KeepAlive(
                timeSeconds = 30
            ),
            isCleanSession = false,
            password = password
        )

        mqttClient.connect(connectOptions)
    }

    private fun initialiseMqtt() {
        val mqttConfig = MqttV3Configuration(
            socketFactory = null,
            logger = getLogger(),
            eventHandler = eventHandler,
            authenticator = object : Authenticator {
                override fun authenticate(
                    connectOptions: MqttConnectOptions,
                    forceRefresh: Boolean
                ): MqttConnectOptions {
                    return connectOptions.copy(password = password.text.toString())
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
                isMqttVersion4Enabled = true
            ),
            pingSender = WorkPingSenderFactory.createMqttPingSender(applicationContext, WorkManagerPingSenderConfig())
        )
        mqttClient = MqttClientFactory.create(this, mqttConfig)

        val configuration = Courier.Configuration(
            client = mqttClient,
            streamAdapterFactories = listOf(RxJava2StreamAdapterFactory()),
            messageAdapterFactories = listOf(GsonMessageAdapterFactory()),
            logger = getLogger()
        )
        val courier = Courier(configuration)
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
