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

[1]: https://medium.com/gojekengineering/courier-library-for-gojeks-information-superhighway-368dc5f052fa
