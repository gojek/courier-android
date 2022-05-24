# Send & Receive messages

Courier library provides the functionality of sending & receiving messages through both service interface and MqttClient.

## Send/Receive using Service Interface

~~~ kotlin
interface MessageService {
	@Receive(topic = "topic/{id}/receive")
	fun receive(@Path("id") identifier: String): Observable<Message>
	
	@Send(topic = "topic/{id}/send", qos = QoS.TWO)
	fun send(@Path("id") identifier: String, @Data message: Message)
}
~~~

~~~ kotlin
messageService.send("user-id", message)

messageService.receive("user-id") { message ->
    print(message)
}
~~~

## Send/Receive using MqttClient

~~~ kotlin
mqttClient.send(message, topic, QoS.TWO)

mqttClient.addMessageListener(topic, object : MessageListener {
    override fun onMessageReceived(mqttMessage: MqttMessage) {
        print(mqttMessage)
    }
})
~~~

**Note** : Only messages for those topics can be received through receive api, which are already subscribed