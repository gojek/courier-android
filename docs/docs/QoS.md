# Quality of Service

The Quality of Service (QoS) level is an agreement between the sender & the receiver of a message that defines the guarantee of delivery for a specific message. There are 3 QoS levels in MQTT:

- At most once (0)
- At least once (1)
- Exactly once (2).

When you talk about QoS in MQTT, you need to consider the two sides of message delivery:

- Message delivery form the publishing client to the broker.
- Message delivery from the broker to the subscribing client.

You can read more about the detail of QoS in MQTT from [HiveMQ site](https://www.hivemq.com/blog/mqtt-essentials-part-6-mqtt-quality-of-service-levels/).