# MQTT Ping Sender

When an MQTT connection between a client and the broker is idle for a long time, it may get torn down due to TCP binding timeout. In order to keep the connection alive, the client needs to send PINGREQ packets through the connection. If the connection is alive, the broker responds with a PINGRESP packet. If the client does not receive the PINGRESP packet within some fixed interval, it breaks the connection and reconnects. The interval at which these packets are sent is the Keepalive Interval.

## Ping Sender

Courier Android library uses Ping Sender for sending pings through the MQTT connection. It encapsulates the actual mechanism used for sending the ping requests.

## Current Implementations

### WorkManagerPingSender

- Uses WorkManager for sending ping requests over the MQTT connection.
- Ideal for cases where the connection needs to be maintained when the app is in background.
- No user permission is required for using this.
- Uses WorkManager version 2.7.0 which requires compileSdkVersion to be 31 or higher.

### Usage

Add this dependency for using WorkManagerPingSender

~~~ kotlin
dependencies {
    implementation "com.gojek.courier:workmanager-pingsender:x.y.z"
}
~~~

Create ping sender using the factory class

~~~ kotlin
pingSender = WorkPingSenderFactory.createMqttPingSender(
                context, workManagerPingSenderConfig
            )
~~~

### WorkManager-2.6.0 PingSender

- Uses WorkManager for sending ping requests over the MQTT connection.
- Ideal for cases where the connection needs to be maintained when the app is in background.
- No user permission is required for using this.
- Uses WorkManager version 2.6.0 which is compatible with apps targeting lower than android 31.

### Usage

Add this dependency for using WorkManagerPingSender

~~~ kotlin
dependencies {
    implementation "com.gojek.courier:workmanager-2.6.0-pingsender:x.y.z"
}
~~~

Create ping sender using the factory class

~~~ kotlin
pingSender = WorkPingSenderFactory.createMqttPingSender(
                context, workManagerPingSenderConfig
            )
~~~

### AlarmPingSender

- Uses Alarms for sending ping requests over the MQTT connection.
- Ideal for cases where the connection needs to be maintained when the app is in background.
- On Android 12 & above, user permission is required for scheduling exact alarms.

### Usage

Add this dependency for using AlarmPingSender

~~~ kotlin
dependencies {
    implementation "com.gojek.courier:alarm-pingsender:x.y.z"
}
~~~

Create ping sender using the factory class

~~~ kotlin
pingSender = AlarmPingSenderFactory.createMqttPingSender(
                context, alarmPingSenderConfig
            )
~~~

### TimerPingSender

- Uses Timer for sending ping requests over the MQTT connection.
- Ideal for cases where the connection needs to be maintained only when the app is in foreground.

### Usage

Add this dependency for using TimerPingSender

~~~ kotlin
dependencies {
    implementation "com.gojek.courier:timer-pingsender:x.y.z"
}
~~~

Create ping sender using the factory class

~~~ kotlin
pingSender = TimerPingSenderFactory.create()
~~~
