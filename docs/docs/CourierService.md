# Courier Service Interface

Courier provides the functionalities like Send, Receive, Subscribe, Unsubscribe through a service interface. This is similar to how we make HTTP calls using Retrofit.

### Usage

Declare a service interface for various actions like Send, Receive, Subscribe, SubscribeMultiple, Unsubscribe.

~~~ kotlin
interface MessageService {
	@Receive(topic = "topic/{id}/receive")
	fun receive(@Path("id") identifier: String): Observable<Message>
	
	@Send(topic = "topic/{id}/send", qos = QoS.TWO)
	fun send(@Path("id") identifier: String, @Data message: Message)
	
	@Subscribe(topic = "topic/{id}/receive", qos = QoS.ONE)
 	fun subscribe(@Path("id") identifier: String): Observable<Message>
	
	@SubscribeMultiple
 	fun subscribe(@TopicMap topicMap: Map<String, QoS>): Observable<Message>
 	
	@Unsubscribe(topics = ["topic/{id}/receive"])
 	fun unsubscribe(@Path("id") identifier: String)
}
~~~



Use Courier to create an implementation of service interface.

~~~ kotlin
val courierConfiguration = Courier.Configuration(
    client = mqttClient,
    streamAdapterFactories = listOf(RxJava2StreamAdapterFactory()),
    messageAdapterFactories = listOf(GsonMessageAdapter.Factory())
)

val courier = Courier(courierConfiguration)

val messageService = courier.create<MessageService>()
~~~

Following annotations are supported for service interface.

- **@Send** : A method annotation used for sending messages over the MQTT connection.

- **@Receive** : A method annotation used for receiving messages over the MQTT connection. Note: The topic needs to be subscribed for receiving messages.

- **@Subscribe** : A method annotation used for subscribing a single topic over the MQTT connection.

- **@SubscribeMultiple** : A method annotation used for subscribing multiple topics over the MQTT connection.

- **@Unsubscribe** : A method annotation used for unsubscribing topics over the MQTT connection.

- **@Path** : A parameter annotation used for specifying a path variable in an MQTT topic.

- **@Data** : A parameter annotation used for specifying the message object while sending a message over the MQTT connection.

- **@TopicMap** : A parameter annotation used for specifying a topic map. It is always used while subscribing multiple topics. 

**Note** : While subscribing topics using `@SubscribeMultiple` along with a stream, make sure that messages received on all topics follow same format or a message adapter is added for handling different format.

### Topic Placeholders

Apart from `@Path` variables (`{...}`) which are substituted from method parameters, a topic can contain **connection placeholders**. These are resolved automatically from the established MQTT connection when the message is actually published/subscribed, so you don't have to pass these values yourself.

| Placeholder | Resolved with |
| --- | --- |
| `%u` | The connection's username |
| `%c` | The connection's client id |

~~~ kotlin
interface MessageService {
	@Send(topic = "user/%u/send", qos = QoS.ONE)
	fun send(@Data message: Message)

	@Subscribe(topic = "client/%c/receive", qos = QoS.ONE)
	fun subscribe(): Observable<Message>
}
~~~

For example, if the connection is established with username `alice` and client id `alice-android`, the topics above resolve to `user/alice/send` and `client/alice-android/receive` respectively.

#### Splitting a placeholder value

A placeholder value can be split by a delimiter and a single part of it can be used in the topic. The syntax is `(<placeholder>,<delimiter>,<index>)` where `<index>` is **0-based**.

~~~ kotlin
@Send(topic = "user/(%c,:,2)/send", qos = QoS.ONE)
fun send(@Data message: Message)
~~~

If the client id is `region:tenant:device`, then `(%c,:,2)` splits it by `:` into `[region, tenant, device]` and picks the part at index `2`, resolving the topic to `user/device/send`. An index that is out of range results in an `IllegalArgumentException`.

Placeholders can be combined freely with `@Path` variables and plain placeholders in the same topic, e.g. `user/(%c,:,0)/{id}/%u`.

**Note** : Topic placeholders are supported in `@Send`, `@Subscribe`, `@SubscribeMultiple` and `@Unsubscribe` topics. They are **not** resolved for `@Receive` topics — a `@Receive` listener is registered against the topic string as-is, so use the already-resolved topic (or `@Subscribe` with a stream) when you need placeholder-based receiving.