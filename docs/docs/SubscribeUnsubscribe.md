# Subscribe & Unsubscribe topics

Courier library provides the functionality of subscribing & unsubscribing topics through both service interface and MqttClient.

## Subscribe/Unsubscribe through Service Interface

~~~ kotlin
interface MessageService {
	@Subscribe(topic = "topic/{id}/receive", qos = QoS.ONE)
 	fun subscribe(@Path("id") identifier: String): Observable<Message>
 	
 	@SubscribeMultiple
 	fun subscribeMultiple(@TopicMap topics: Map<String, QoS>): Observable<Message>
 	
	@Unsubscribe(topics = ["topic/{id}/receive"])
 	fun unsubscribe(@Path("id") identifier: String)
}
~~~

~~~ kotlin
messageService.subscribe("user-id").subscribe { message ->
    print(message)
}

messageService.subscribeMultiple(mapOf("topic1" to QoS.ONE, "topic2" to QoS.TWO)).subscribe { message ->
    print(message)
}

messageService.unsubscribe("user-id")
~~~

## Subscribe/Unsubscribe through MqttClient

~~~ kotlin
mqttClient.subscribe("topic1" to QoS.ZERO, "topic2" to QoS.ONE)

mqttClient.unsubscribe("topic1", "topic2")
~~~

**Note** : While subscribing topics using `@SubscribeMultiple` along with a stream, make sure that messages received on all topics follow same format or a message adapter is added for handling different format.


