![Build and Smoketest Status](https://github.com/gojek/courier-android/actions/workflows/android.yml/badge.svg)
<a href="https://gojek.github.io/courier-android/">
	<img alt="Documentation" src="https://img.shields.io/badge/documentation-yes-brightgreen.svg" target="_blank" />
</a>
<a href="https://github.com/gojek/courier-android/graphs/commit-activity">
	<img alt="Maintenance" src="https://img.shields.io/badge/maintained-yes-green.svg" target="_blank" />
</a>
<a href="https://github.com/gojek/courier-android/releases/latest">
	<img alt="GitHub Release Date" src="https://img.shields.io/github/release-date/gojek/courier-android">
</a>
<a href="https://github.com/gojek/courier-android/commits/main">
	<img alt="GitHub last commit" src="https://img.shields.io/github/last-commit/gojek/courier-android">
</a>
[![Discord : Gojek Courier](https://img.shields.io/badge/Discord-Gojek%20Courier-blue.svg)](https://discord.gg/C823qK4AK7)

<p align="center">
<img src="https://github.com/gojek/courier-android/blob/main/docs/static/img/courier-logo-full-black.svg#gh-light-mode-only" width="500"/>
</p>

<p align="center">
<img src="https://github.com/gojek/courier-android/blob/main/docs/static/img/courier-logo-full-white.svg#gh-dark-mode-only" width="500"/>
</p>

## About Courier

Courier is a kotlin library for creating long running connections using MQTT protocol.

Long running connection is a persistent connection established between client & server for instant bi-directional communication. A long running connection is maintained for maximum possible duration with the help of keep alive packets. This helps in saving battery and data on mobile devices.

MQTT is an extremely lightweight protocol which works on publish/subscribe messaging model. It is designed for connections with remote locations where a "small code footprint" is required or the network bandwidth is limited.

The protocol usually runs over TCP/IP; however, any network protocol that provides ordered, lossless, bi-directional connections can support MQTT.

MQTT has 3 built-in QoS levels for Reliable Message Delivery:

* **QoS 0(At most once)** - the message is sent only once and the client and broker take no additional steps to acknowledge delivery (fire and forget).

* **QoS 1(At least once)** - the message is re-tried by the sender multiple times until acknowledgement is received (acknowledged delivery).

* **QoS 2(Exactly once)** - the sender and receiver engage in a two-level handshake to ensure only one copy of the message is received (assured delivery).

## Detailed Documentation

Find the detailed documentation here - https://gojek.github.io/courier-android/

End-to-end courier example - https://gojek.github.io/courier/docs/Introduction

## Features

* Clean API

* Adaptive Keep Alive

* Message & Stream Adapters

* Subscription Store

* Automatic Reconnect & Resubscribe

* Database Persistence

* Backpressure handling

* Alarm, Timer & WorkManager Ping Sender

* MQTT Chuck

More details about features in Courier library can be found [here][1]

## Getting Started

### Sample App

A demo application is added [here](./app/src/main/java/com/gojek/courier/app/ui/MainActivity.kt) which makes Courier connection with a [HiveMQ][2] public broker.

### Download
[![Maven Central](https://img.shields.io/maven-central/v/com.gojek.courier/courier.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22com.gojek.courier%22%20AND%20a:%courier%22)

All artifacts of Courier library are available via Maven Central.

```groovy
repositories {
    mavenCentral()
}

dependencies {
    implementation "com.gojek.courier:courier:x.y.z"

    implementation "com.gojek.courier:courier-message-adapter-gson:x.y.z"
    implementation "com.gojek.courier:courier-stream-adapter-rxjava2:x.y.z"
}
```

### Usage

Declare a service interface for actions like Send, Receive, Subscribe, Unsubscribe:

~~~ kotlin
interface MessageService {
	@Receive(topic = "topic/{id}/receive")
	fun receive(@Path("id") identifier: String): Observable<Message>

	@Send(topic = "topic/{id}/send", qos = QoS.TWO)
	fun send(@Path("id") identifier: String, @Data message: Message)

	@Subscribe(topic = "topic/{id}/receive", qos = QoS.ONE)
 	fun subscribe(@Path("id") identifier: String): Observable<Message>

	@Unsubscribe(topics = ["topic/{id}/receive"])
 	fun unsubscribe(@Path("id") identifier: String)
}
~~~

Use Courier to create an implementation:

~~~ kotlin
val mqttClient = MqttClientFactory.create(
    context = context,
    mqttConfiguration = MqttV3Configuration(
        authenticator = authenticator
    )
)

val courierConfiguration = Courier.Configuration(
    client = mqttClient,
    streamAdapterFactories = listOf(RxJava2StreamAdapterFactory()),
    messageAdapterFactories = listOf(GsonMessageAdapter.Factory())
)

val courier = Courier(courierConfiguration)

val messageService = courier.create<MessageService>()
~~~

### Subscribe/Unsubscribe using Service Interface

~~~ kotlin
messageService.subscribe("user-id").subscribe { message ->
    print(message)
}
messageService.unsubscribe("user-id")
~~~

### Send/Receive using Service Interface

~~~ kotlin
messageService.send("user-id", message)
messageService.receive("user-id") { message ->
    print(message)
}
~~~

### Connect using MqttClient

~~~ kotlin
val connectOptions = MqttConnectOptions(
    serverUris = listOf(ServerUri(SERVER_URI, SERVER_PORT)),
    clientId = clientId,
    username = username,
    keepAlive = KeepAlive(
        timeSeconds = keepAliveSeconds
    ),
    isCleanSession = cleanSessionFlag,
    password = password
)

mqttClient.connect(connectOptions)
~~~

### Disconnect using MqttClient

~~~ kotlin
mqttClient.disconnect()
~~~

## Non-standard Connection options

### UserProperties in MqttConnectionOptions

This option allows you to send user-properties in CONNECT packet for MQTT v3.1.1.

~~~ kotlin
val connectOptions = MqttConnectOptions(
    serverUris = listOf(ServerUri(SERVER_URI, SERVER_PORT)),
    clientId = clientId,
    ...
    userPropertiesMap = mapOf(
                "key1" to "value1",
                "key2" to "value2"
    )
)

mqttClient.connect(connectOptions)
~~~

:warning: **
This is a non-standard option. As far as the MQTT specification is concerned, user-properties support is added in MQTT v5. So to support this in MQTT v3.1.1, broker needs to have support for this as well.

## Contribution Guidelines

Read our [contribution guide](./CONTRIBUTION.md) to learn about our development process, how to propose bugfixes and improvements, and how to build and test your changes to Courier Android library.

## License

All Courier modules except Paho are [MIT Licensed](./LICENSES/LICENSE). Paho is [Eclipse Licensed](./LICENSES/LICENSE.paho).

[1]: https://medium.com/gojekengineering/courier-library-for-gojeks-information-superhighway-368dc5f052fa
[2]: https://broker.mqttdashboard.com/
