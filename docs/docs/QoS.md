# Quality of Service

The Quality of Service (QoS) level is an agreement between the sender & the receiver of a message that defines the guarantee of delivery for a specific message. There are 3 QoS levels in MQTT:

- At most once (0)
- At least once (1)
- Exactly once (2).

When you talk about QoS in MQTT, you need to consider the two sides of message delivery:

- Message delivery form the publishing client to the broker.
- Message delivery from the broker to the subscribing client.

You can read more about the detail of QoS in MQTT from [HiveMQ site](https://www.hivemq.com/blog/mqtt-essentials-part-6-mqtt-quality-of-service-levels/).

:warning: **
These are non standard QoS options. You need to have compatible broker to use these QoS options

We added two more Qos options

- QoS1 with no persistence and no retry: Like QoS1, Message delivery is acknowledged with PubAck, but unlike Qos1 messages, these are
  neither persisted and nor retried at send after single attempt. The message arrives at the receiver either once or not at all
- QoS1 with no persistence and with retry: Like QoS1, Message delivery is acknowledged with PubAck, but unlike Qos1 messages, these are
  not persisted. The messages are retried within current session if delivery is not acknowledged