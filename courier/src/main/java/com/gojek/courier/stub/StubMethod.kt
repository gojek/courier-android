package com.gojek.courier.stub

import com.gojek.courier.MessageAdapter
import com.gojek.courier.QoS
import com.gojek.courier.StreamAdapter
import com.gojek.courier.annotation.parser.MethodAnnotationsParser
import com.gojek.courier.argument.processor.ReceiveArgumentProcessor
import com.gojek.courier.argument.processor.SendArgumentProcessor
import com.gojek.courier.argument.processor.SubscriptionArgumentProcessor
import com.gojek.courier.argument.processor.UnsubscriptionArgumentProcessor
import com.gojek.courier.utils.MessageAdapterResolver
import com.gojek.courier.utils.StreamAdapterResolver
import java.lang.reflect.Method

internal sealed class StubMethod {

    class Send(
        val messageAdapter: MessageAdapter<Any>,
        val qos: QoS,
        val argumentProcessor: SendArgumentProcessor
    ) : StubMethod()

    class Receive(
        val messageAdapter: MessageAdapter<Any>,
        val streamAdapter: StreamAdapter<Any, Any>,
        val argumentProcessor: ReceiveArgumentProcessor
    ) : StubMethod()

    class Subscribe(
        val qos: QoS,
        val argumentProcessor: SubscriptionArgumentProcessor
    ) : StubMethod()

    class SubscribeWithStream(
        val qos: QoS,
        val argumentProcessor: SubscriptionArgumentProcessor,
        val messageAdapter: MessageAdapter<Any>,
        val streamAdapter: StreamAdapter<Any, Any>
    ) : StubMethod()

    object SubscribeAll : StubMethod()

    class SubscribeAllWithStream(
        val messageAdapter: MessageAdapter<Any>,
        val streamAdapter: StreamAdapter<Any, Any>
    ) : StubMethod()

    class Unsubscribe(
        val argumentProcessor: UnsubscriptionArgumentProcessor
    ) : StubMethod()

    class Factory(
        private val streamAdapterResolver: StreamAdapterResolver,
        private val messageAdapterResolver: MessageAdapterResolver
    ) {

        fun create(method: Method): StubMethod? {
            val annotationsParser = MethodAnnotationsParser(
                method,
                streamAdapterResolver,
                messageAdapterResolver
            )
            return annotationsParser.stubMethod
        }
    }
}
