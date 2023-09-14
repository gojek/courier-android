package com.gojek.courier.stub

import com.gojek.courier.Stream
import com.gojek.courier.utils.RuntimePlatform
import com.gojek.mqtt.client.model.ConnectionState
import com.gojek.mqtt.event.MqttEvent
import java.lang.reflect.Method

internal class StubInterface(
    val stubMethods: Map<Method, StubMethod>,
    private val callback: Callback
) {
    fun invoke(method: Method, args: Array<Any>): Any {
        val stubMethod = checkNotNull(stubMethods[method]) { "Stub method not found" }
        return when (stubMethod) {
            is StubMethod.Send -> {
                callback.send(stubMethod, args)
            }
            is StubMethod.Receive -> {
                callback.receive(stubMethod, args)
            }
            is StubMethod.Subscribe -> {
                callback.subscribe(stubMethod, args)
            }
            is StubMethod.SubscribeWithStream -> {
                callback.subscribeWithStream(stubMethod, args)
            }
            is StubMethod.SubscribeAll -> {
                callback.subscribeAll(stubMethod, args)
            }
            is StubMethod.SubscribeAllWithStream -> {
                callback.subscribeAllWithStream(stubMethod, args)
            }
            is StubMethod.Unsubscribe -> {
                callback.unsubscribe(stubMethod, args)
            }
        }
    }

    interface Callback {
        fun send(stubMethod: StubMethod.Send, args: Array<Any>): Any
        fun receive(stubMethod: StubMethod.Receive, args: Array<Any>): Any
        fun subscribe(stubMethod: StubMethod.Subscribe, args: Array<Any>): Any
        fun subscribeWithStream(stubMethod: StubMethod.SubscribeWithStream, args: Array<Any>): Any
        fun unsubscribe(stubMethod: StubMethod.Unsubscribe, args: Array<Any>): Any
        fun subscribeAll(stubMethod: StubMethod.SubscribeAll, args: Array<Any>): Any
        fun subscribeAllWithStream(stubMethod: StubMethod.SubscribeAllWithStream, args: Array<Any>): Any
        fun getEventStream(): Stream<MqttEvent>
        fun getConnectionState(): ConnectionState
    }

    internal class Factory(
        private val runtimePlatform: RuntimePlatform,
        private val callback: Callback, // Coordinator implements this callback
        private val stubMethodFactory: StubMethod.Factory
    ) {

        fun <T> create(anInterface: Class<T>): StubInterface {
            validateInterface(anInterface)
            val stubMethods = anInterface.findStubMethods()
            return StubInterface(stubMethods, callback)
        }

        private fun Class<*>.findStubMethods(): Map<Method, StubMethod> {
            // Remove all default methods
            val methods = declaredMethods.filterNot { runtimePlatform.isDefaultMethod(it) }
            require(methods.isNotEmpty()) { "Service interface should have atleast one abstract method" }
            val stubMethods = methods.mapNotNull { stubMethodFactory.create(it) }
            return methods.zip(stubMethods).toMap()
        }

        private fun validateInterface(anInterface: Class<*>) {
            require(anInterface.isInterface) { "Service declarations must be interfaces." }

            // Prevent API interfaces from extending other interfaces. This not only avoids a bug in
            // Android (http://b.android.com/58753) but it forces composition of API declarations which is
            // the recommended pattern.
            require(anInterface.interfaces.isEmpty()) { "Service interfaces must not extend other interfaces." }
        }
    }
}
