# MQTT Chuck

MQTT Chuck is used for inspecting all the outgoing or incoming MQTT packets for an underlying MQTT connection. MQTT Chuck is similar to [HTTP Chuck][1], used for inspecting the HTTP calls on an android application.

MQTT Chuck uses an interceptor to intercept all the MQTT packets, persisting them and providing a UI for accessing all the MQTT packets sent or received over the MQTT connection. It also provides multiple other features like search, share, and clear data.

![image chuck](./../static/img/mqtt-chuck.png)

## Usage

Add this dependency for using MQTT chuck

~~~ kotlin
dependencies {
    implementation "com.gojek.courier:chuck-mqtt:x.y.z"
}
~~~

To enable MQTT chuck for your courier connection, just pass the `MqttChuckInterceptor` inside [MqttConfiguration](MqttConfiguration)

~~~ kotlin
mqttConfiguration = MqttV3Configuration(
    mqttInterceptorList = listOf(MqttChuckInterceptor(context, mqttChuckConfig))
)
~~~

[1]: https://github.com/jgilfelt/chuck