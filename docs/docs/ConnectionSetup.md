# Connection Setup

## MqttClient

An instance of [MqttClient][1] needs to be created in order to establish a Courier connection.

~~~ kotlin
val mqttClient = MqttClientFactory.create(
    context = context,
    mqttConfiguration = mqttConfiguration
)
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

### MqttConnectOptions

[MqttConnectOptions][2] represents the properties of the underlying MQTT connection in Courier.

- **Server URIs** : List of ServerUri representing the host and port of an MQTT broker.

- **Client Id** : Unique ID of the MQTT client.

- **Username** : Username of the MQTT client.

- **Password** : Password of the MQTT client.

- **KeepAlive Interval** : Interval at which keep alive packets are sent for the MQTT connection.

- **Clean Session Flag** : When clean session is false, a persistent connection is created. Otherwise, non-persistent connection is created and all persisted information is cleared from both client and broker.

- **Read Timeout** : Read timeout of the SSL/TCP socket created for the MQTT connection.

- **MQTT protocol version** : It can be either VERSION_3_1 or VERSION_3_1_1.

- **User properties** : Custom user properties appended to the CONNECT packet.

[1]: https://github.com/gojek/courier-android/blob/main/mqtt-client/src/main/java/com/gojek/mqtt/client/MqttClient.kt
[2]: https://github.com/gojek/courier-android/blob/main/mqtt-client/src/main/java/com/gojek/mqtt/model/MqttConnectOptions.kt
[3]: https://github.com/gojek/courier-android/blob/main/mqtt-client/src/main/java/com/gojek/mqtt/model/ServerUri.kt
