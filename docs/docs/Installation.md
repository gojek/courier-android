# Installation

## Supported SDK versions

- minSdkVersion: 21
- targetSdkVersion: 34
- compileSdkVersion: 34

## Download
[![Maven Central](https://img.shields.io/maven-central/v/com.gojek.courier/courier.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22com.gojek.courier%22%20AND%20a:%courier%22)

All artifacts of Courier library are available via Maven Central.

~~~ kotlin
repositories {
    mavenCentral()
}

dependencies {
    implementation "com.gojek.courier:courier:x.y.z"

    implementation "com.gojek.courier:courier-message-adapter-gson:x.y.z"
    implementation "com.gojek.courier:courier-stream-adapter-rxjava2:x.y.z"
}
~~~

## Modules

Courier Android library provides multiple use case specific modules

### Core modules

These modules provide the core functionalities like Connect/Disconnect, Subscribe/Unsubscribe, Send/Receive

- courier
- mqtt-client

### Message & Stream Adapters

Library provided implementations of message and stream adapters. Read more about them [here](MessageStreamAdapters).

- courier-message-adapter-gson
- courier-message-adapter-moshi
- courier-message-adapter-protobuf
- courier-stream-adapter-rxjava
- courier-stream-adapter-rxjava2
- courier-stream-adapter-coroutines

### Ping Sender

Library provided implementations of Mqtt Ping Sender. Read more about them [here](PingSender).

- timer-pingsender
- workmanager-pingsender
- workmanager-2.6.0-pingsender
- alarm-pingsender

### Http Authenticator

Library provided implementation of Authenticator. Read more about this [here](Authenticator).

- courier-auth-http

### MQTT Chuck

HTTP Chuck inspired tool for debugging all MQTT packets. Read more about this [here](MqttChuck).

- chuck-mqtt
