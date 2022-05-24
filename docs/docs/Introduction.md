# Introduction

![image banner](./../static/img/courier-logo-full-black.svg#gh-light-mode-only)
![image banner](./../static/img/courier-logo-full-white.svg#gh-dark-mode-only)
## About Courier

Courier is a kotlin library for creating long running connections using MQTT protocol.

Long running connection is a persistent connection established between client & server for instant bi-directional communication. A long running connection is maintained for maximum possible duration with the help of keep alive packets. This helps in saving battery and data on mobile devices.

MQTT is an extremely lightweight protocol which works on publish/subscribe messaging model. It is designed for connections with remote locations where a "small code footprint" is required or the network bandwidth is limited.

The protocol usually runs over TCP/IP; however, any network protocol that provides ordered, lossless, bi-directional connections can support MQTT.

MQTT has 3 built-in QoS levels for Reliable Message Delivery:

* **QoS 0(At most once)** - the message is sent only once and the client and broker take no additional steps to acknowledge delivery (fire and forget).

* **QoS 1(At least once)** - the message is re-tried by the sender multiple times until acknowledgement is received (acknowledged delivery).

* **QoS 2(Exactly once)** - the sender and receiver engage in a two-level handshake to ensure only one copy of the message is received (assured delivery).

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

Read our [contribution guide](CONTRIBUTION) to learn about our development process, how to propose bugfixes and improvements, and how to build and test your changes to Courier Android library.

## License

All Courier modules except Paho are [MIT Licensed](LICENSE). Paho is [Eclipse Licensed](LICENSE.paho).

[1]: https://medium.com/gojekengineering/courier-library-for-gojeks-information-superhighway-368dc5f052fa
[2]: https://broker.mqttdashboard.com/
[3]: https://github.com/gojek/courier-android/tree/main/app
