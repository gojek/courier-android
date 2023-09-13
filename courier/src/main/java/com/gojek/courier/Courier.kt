package com.gojek.courier

import com.gojek.courier.coordinator.Coordinator
import com.gojek.courier.logging.ILogger
import com.gojek.courier.logging.NoOpLogger
import com.gojek.courier.stub.ProxyFactory
import com.gojek.courier.stub.StubInterface
import com.gojek.courier.stub.StubMethod
import com.gojek.courier.utils.MessageAdapterResolver
import com.gojek.courier.utils.RuntimePlatform
import com.gojek.courier.utils.StreamAdapterResolver
import com.gojek.mqtt.client.MqttClient
import com.gojek.mqtt.client.model.ConnectionState
import com.gojek.mqtt.event.MqttEvent

class Courier(private val configuration: Configuration) {
    private val stubInterfaceFactory: StubInterface.Factory
    private val proxyFactory: ProxyFactory
    private val coordinator: Coordinator

    init {
        coordinator = Coordinator(configuration.client, configuration.logger)
        val messageAdapterResolver = configuration.createMessageAdapterResolver()
        val streamAdapterResolver = configuration.createStreamAdapterResolver()
        stubInterfaceFactory = StubInterface.Factory(
            RuntimePlatform.get(),
            coordinator,
            StubMethod.Factory(
                streamAdapterResolver,
                messageAdapterResolver
            )
        )

        proxyFactory = ProxyFactory(RuntimePlatform.get())
    }

    fun <T> create(service: Class<T>): T {
        val stubInterface = stubInterfaceFactory.create(service)
        return proxyFactory.create(service, stubInterface)
    }

    /**
     * Same as [create].
     */
    inline fun <reified T : Any> create(): T = create(T::class.java)

    fun getEventStream(): Stream<MqttEvent> {
        return coordinator.getEventStream()
    }

    fun getConnectionState(): ConnectionState {
        return coordinator.getConnectionState()
    }

    fun newBuilder(): Builder {
        return Builder(configuration)
    }

    data class Configuration(
        val client: MqttClient,
        val streamAdapterFactories: List<StreamAdapter.Factory> = emptyList(),
        val messageAdapterFactories: List<MessageAdapter.Factory> = emptyList(),
        val logger: ILogger = NoOpLogger()
    )

    private fun Configuration.createStreamAdapterResolver(): StreamAdapterResolver {
        return StreamAdapterResolver(streamAdapterFactories)
    }

    private fun Configuration.createMessageAdapterResolver(): MessageAdapterResolver {
        return MessageAdapterResolver(messageAdapterFactories)
    }

    class Builder(private var configuration: Configuration) {

        fun addMessageAdapterFactories(messageAdapterFactories: List<MessageAdapter.Factory>): Builder {
            configuration = configuration.copy(
                messageAdapterFactories = configuration.messageAdapterFactories + messageAdapterFactories
            )
            return this
        }

        fun addStreamAdapterFactories(streamAdapterFactories: List<StreamAdapter.Factory>): Builder {
            configuration = configuration.copy(
                streamAdapterFactories = configuration.streamAdapterFactories + streamAdapterFactories
            )
            return this
        }

        fun build(): Courier {
            return Courier(configuration)
        }
    }
}
