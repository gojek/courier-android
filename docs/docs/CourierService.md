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