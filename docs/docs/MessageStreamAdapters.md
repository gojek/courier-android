# Message & Stream Adapters

Courier provides the functionality of passing your own custom or library-provided message & stream adapters.

## Message Adapter

To serialize and deserialize received and published messages, Courier uses MessageAdapter. With this, you don't need to handle the serialization and deserialization process when publishing and receiving messages from broker.

Courier library provides the following message adapters:

- [Gson](https://github.com/google/gson): courier-message-adapter-gson

- [Kotlin Serialization](https://github.com/Kotlin/kotlinx.serialization): courier-message-adapter-kotlin-serialization

- [Moshi](https://github.com/square/moshi/): courier-message-adapter-moshi

- [Protobuf](https://developers.google.com/protocol-buffers/): courier-message-adapter-protobuf

You can also create your own custom message adapter by implementing the MessageAdapter.Factory interface.

~~~ kotlin
class MyCustomMessageAdapterFactory : MessageAdapter.Factory {

    override fun create(type: Type, annotations: Array<Annotation>): MessageAdapter<*> {
        return MyCustomMessageAdapter()
    }
}

private class MyCustomMessageAdapter<T> constructor() : MessageAdapter<T> {

    override fun fromMessage(topic: String, message: Message): T {
        // convert message to custom type
    }

    override fun toMessage(topic: String, data: T): Message {
        // convert custom type to message
    }

    override fun contentType(): String {
        // content-type supported by this adapter.
    }
}
~~~

## Stream Adapter

Courier library provides the following stream adapters:

- courier-stream-adapter-rxjava

- courier-stream-adapter-rxjava2

- courier-stream-adapter-coroutines

You can also create your own custom Stream adapter by implementing the StreamAdapter.Factory interface.

~~~ kotlin
class MyCustomStreamAdapterFactory : StreamAdapter.Factory {

    override fun create(type: Type): StreamAdapter<Any, Any> {
        return MyCustomStreamAdapter()
    }
}

private class MyCustomStreamAdapter<T> : StreamAdapter<T, Any> {

    override fun adapt(stream: Stream<T>): Any {
        // convert stream to custom stream
    }
}
~~~
