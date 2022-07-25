# Experiment Configs

These are the experimentation configs used in Courier library. These are volatile configs i.e., they can be modified/moved/removed in future.

- **isPersistentSubscriptionStoreEnabled** : When enabled, `PersistableSubscriptionStore` implementation of `SubscriptionStore` is used. Otherwise, `InMemorySubscriptionStore` is used. Read more about [SubscriptionStore](SubscriptionStore)

- **adaptiveKeepAliveConfig** : This config is used for enabling [Adaptive KeepAlive](AdaptiveKeepAlive) feature in courier library.

- **activityCheckIntervalSeconds** : Interval at which channel activity is checked for unacknowledged MQTT packets.

- **inactivityTimeoutSeconds** : When acknowledgement for an MQTT packet is not received within this interval, the connection is reestablished.

- **policyResetTimeSeconds** : After this interval, connect retry policy is reset once the connection is successfully made.

- **incomingMessagesTTLSecs** : When there is no listener attached for an incoming message, messages are persisted for this interval.

- **incomingMessagesCleanupIntervalSecs** : Interval at which cleanup for incoming messages persistence is performed.
  
- **isRunnableSchedulerV2Enabled** : When this config is enabled, a new implementation of [IRunnableScheduler][1] is used for scheduling tasks like connect & disconnect. This new implementation stops the thread used internally when destroy api of [MqttClient][2] is invoked, which is started again when connect api is invoked.

[1]: https://github.com/gojek/courier-android/blob/main/mqtt-client/src/main/java/com/gojek/mqtt/scheduler/IRunnableScheduler.kt
[2]: https://github.com/gojek/courier-android/blob/main/mqtt-client/src/main/java/com/gojek/mqtt/client/MqttClient.kt