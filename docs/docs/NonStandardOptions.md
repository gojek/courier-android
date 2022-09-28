# Non-standard Connection options

### UserProperties in MqttConnectionOptions

This option allows you to send user-properties in CONNECT packet for MQTT v3.1.1.

~~~ kotlin
val connectOptions = MqttConnectOptions.Builder()
              .serverUris(listOf(ServerUri(SERVER_URI, SERVER_PORT)))
              .clientId(clientId)
              ...
              .userProperties(
                  userProperties = mapOf(
                    "key1" to "value1",
                    "key2" to "value2"
                  )
              )
              .build()

mqttClient.connect(connectOptions)
~~~

:warning: **
This is a non-standard option. As far as the MQTT specification is concerned, user-properties support is added in MQTT v5. So to support this in MQTT v3.1.1, broker needs to have support for this as well.