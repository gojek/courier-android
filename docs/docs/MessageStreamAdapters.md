# Message & Stream Adapters

Courier provides the functionality of passing your own custom or library-provided message & stream adapters.

## Message Adapter

To serialize and deserialize received and published messages, Courier uses MessageAdapter. With this, you don't need to handle the serialization and deserialization process when publishing and receiving messages from broker.

Courier library provides the following message adapters:

- courier-message-adapter-gson

- courier-message-adapter-moshi

- courier-message-adapter-protobuf

You can also create your own custom message adapter by implementing the MessageAdapter interface.

// Add example

## Stream Adapter

Courier library provides the following stream adapters:

- courier-stream-adapter-rxjava

- courier-stream-adapter-rxjava2

- courier-stream-adapter-coroutines

You can also create your own custom Stream adapter by implementing the StreamAdapter interface.

// Add example
