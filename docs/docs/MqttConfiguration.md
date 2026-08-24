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

- **Persistence Options** : It allows you to configure message persistence and the offline buffer present inside Paho.
    - **shouldUseMemoryPersistence** : When enabled, an in-memory, capacity-bounded [Bounded Memory Persistence][4] is used for inflight/QoS message persistence instead of the default disk-based persistence.
    - **memoryPersistenceCapacity** : Maximum number of entries Bounded Memory Persistence holds when `shouldUseMemoryPersistence` is enabled. Once the limit is reached, new messages are either dropped or used to evict the oldest entry, depending on `isDeleteOldestMessages`.
    - **bufferCapacity** : Maximum number of messages the offline buffer can hold while the client is disconnected.
    - **isPersistBuffer** : When enabled, offline-buffered messages are also written through the active `MqttClientPersistence` (memory or disk), sharing capacity with inflight/QoS message persistence. Disable this to keep the offline buffer independent of that persistence store.
    - **isDeleteOldestMessages** : When the offline buffer or Bounded Memory Persistence is full, enabling this evicts the oldest entry to make room for the new one. When disabled, the offline buffer rejects the new message with an exception, while Bounded Memory Persistence silently drops it instead of throwing.

- **Experimentation Configs** : These are the experiment configs used inside Courier library which are explained in detail [here](ExperimentConfigs).

- **WakeLock Timeout** : When positive value of this timeout is passed, a wakelock is acquired while creating the MQTT connection. By default, it is 0. 

[1]: https://github.com/gojek/courier-android/blob/main/mqtt-client/src/main/java/com/gojek/mqtt/client/MqttClient.kt
[2]: https://github.com/gojek/courier-android/blob/main/mqtt-client/src/main/java/com/gojek/mqtt/client/config/v3/MqttV3Configuration.kt
[3]: https://github.com/gojek/courier-android/blob/main/pingsender/mqtt-pingsender/src/main/java/com/gojek/mqtt/pingsender/MqttPingSender.kt
[4]: https://github.com/gojek/courier-android/blob/main/paho/src/main/java/org/eclipse/paho/client/mqttv3/persist/BoundedMemoryPersistence.java
