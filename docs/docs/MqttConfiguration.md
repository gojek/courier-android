# MQTT Client Configuration

As we have seen earlier, [MqttClient][1] requires an instance of [MqttV3Configuration][2]. MqttV3Configuration allows you to configure the following properties of MqttClient:

## Required Configs

- **MqttPingSender** : It is an implementation of [MqttPingSender][3] interface, which defines the logic of sending ping requests over the MQTT connection. Read more ping sender [here](PingSender).

- **Authenticator** : MqttClient uses Authenticator to refresh the connect options when username or password are incorrect. Read more Authenticator [here](Authenticator).

## Optional Configs

- **Retry Policies** : There are multiple retry policies used in Courier library - connect retry policy, connect timeout policy, subscription policy. You can either use the in-built policies or provide your own custom policies.

- **Logger** : An instance of ILogger can be passed to get the internal logs.

- **Event Handler** : EventHandler allows you to listen to all the library events like connect attempt/success/failure, message send/receive, subscribe/unsubscribe.

- **Mqtt Interceptors** : By passing mqtt interceptors, you can intercept all the MQTT packets sent over the courier connection. This is also used for enabling [MQTT Chuck](MqttChuck).

- **Persistence Options** : It allows you to configure the offline buffer present inside Paho. This buffer is used for storing all the messages while the client is offline.

- **Experimentation Configs** : These are the experiment configs used inside Courier library which are explained in detail [here](ExperimentConfigs).

- **WakeLock Timeout** : When positive value of this timeout is passed, a wakelock is acquired while creating the MQTT connection. By default, it is 0. 

[1]: https://github.com/gojek/courier-android/blob/main/mqtt-client/src/main/java/com/gojek/mqtt/client/MqttClient.kt
[2]: https://github.com/gojek/courier-android/blob/main/mqtt-client/src/main/java/com/gojek/mqtt/client/config/v3/MqttV3Configuration.kt
[3]: https://github.com/gojek/courier-android/blob/main/pingsender/mqtt-pingsender/src/main/java/com/gojek/mqtt/pingsender/MqttPingSender.kt
