package com.gojek.courier.annotation.parser

import com.gojek.courier.QoS
import com.gojek.courier.annotation.Callback
import com.gojek.courier.annotation.Data
import com.gojek.courier.annotation.Path
import com.gojek.courier.annotation.Receive
import com.gojek.courier.annotation.Send
import com.gojek.courier.annotation.Subscribe
import com.gojek.courier.annotation.SubscribeMultiple
import com.gojek.courier.annotation.TopicMap
import com.gojek.courier.annotation.Unsubscribe
import com.gojek.courier.argument.processor.ReceiveArgumentProcessor
import com.gojek.courier.argument.processor.SendArgumentProcessor
import com.gojek.courier.argument.processor.SubscriptionArgumentProcessor
import com.gojek.courier.argument.processor.UnsubscriptionArgumentProcessor
import com.gojek.courier.callback.SendMessageCallback
import com.gojek.courier.stub.StubMethod
import com.gojek.courier.utils.MessageAdapterResolver
import com.gojek.courier.utils.StreamAdapterResolver
import com.gojek.courier.utils.getParameterUpperBound
import com.gojek.courier.utils.getRawType
import com.gojek.courier.utils.hasUnresolvableType
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.collections.LinkedHashMap

internal class MethodAnnotationsParser(
    method: Method,
    private val streamAdapterResolver: StreamAdapterResolver,
    private val messageAdapterResolver: MessageAdapterResolver
) {
    var stubMethod: StubMethod? = null
    private val pathMap = mutableMapOf<String, Int>()

    init {
        val annotations = method.annotations.filter { it.isStubMethodAnnotation() }
        require(annotations.size == 1) {
            "A method must have one and only one service method annotation: $this"
        }

        when (annotations.first()) {
            is Receive -> parseReceiveMethodAnnotations(method)
            is Send -> parseSendMethodAnnotations(method)
            is Subscribe -> parseSubscribeMethodAnnotations(method)
            is SubscribeMultiple -> parseSubscribeAllMethodAnnotations(method)
            is Unsubscribe -> parseUnsubscribeMethodAnnotations(method)
        }
    }

    private fun parseReceiveMethodAnnotations(method: Method) {
        method.requireReturnTypeIsOneOf(ParameterizedType::class.java) {
            "Receive method must return ParameterizedType: $method"
        }
        method.requireReturnTypeIsResolvable {
            "Method return type must not include a type variable or wildcard: ${method.genericReturnType}"
        }

        val annotation = method.annotations.first()
        val topic = (annotation as Receive).topic
        buildPathMap(topic, method)
        val streamType = method.genericReturnType as ParameterizedType
        val messageType = streamType.getFirstTypeArgument()
        val annotations = method.annotations

        val streamAdapter = streamAdapterResolver.resolve(streamType)
        val messageAdapter = messageAdapterResolver.resolve(messageType, annotations)
        val argumentProcessor = ReceiveArgumentProcessor(pathMap, topic)
        stubMethod = StubMethod.Receive(
            messageAdapter,
            streamAdapter,
            argumentProcessor
        )
    }

    private fun parseSendMethodAnnotations(method: Method) {
        method.requireReturnTypeIsOneOf(Boolean::class.java, Void.TYPE) {
            "Send method must return Boolean or Void: $method"
        }

        val annotation = method.annotations.first()
        val topic = (annotation as Send).topic
        buildPathMap(topic, method)
        val qos = annotation.qos
        val dataParameterIndex = method.getDataParameterIndex()
        val messageType = method.getDataParameterType(dataParameterIndex)
        val annotations = method.getDataParameterAnnotations(dataParameterIndex)
        val adapter = messageAdapterResolver.resolve(messageType, annotations)
        val callbackIndex = method.getCallbackParameterIndex()
        val argumentProcessor = SendArgumentProcessor(pathMap, topic, dataParameterIndex, callbackIndex)
        stubMethod = StubMethod.Send(adapter, qos, argumentProcessor)
    }

    private fun parseSubscribeMethodAnnotations(method: Method) {
        method.requireReturnTypeIsOneOf(Void.TYPE, ParameterizedType::class.java) {
            "Subscribe method must return Void or ParameterizedType: $method"
        }
        if (method.genericReturnType == Void.TYPE) {
            parseSubscribeWithoutStreamMethodAnnotations(method)
        } else {
            parseSubscribeWithStreamMethodAnnotations(method)
        }
    }

    private fun parseSubscribeWithoutStreamMethodAnnotations(method: Method) {
        val annotation = method.annotations.first()
        val topic = (annotation as Subscribe).topic
        buildPathMap(topic, method)
        val qos = annotation.qos
        val argumentProcessor = SubscriptionArgumentProcessor(pathMap, topic)

        stubMethod = StubMethod.Subscribe(qos, argumentProcessor)
    }

    private fun parseSubscribeWithStreamMethodAnnotations(method: Method) {
        method.requireReturnTypeIsResolvable {
            "Method return type must not include a type variable or wildcard: ${method.genericReturnType}"
        }

        val annotation = method.annotations.first()
        val topic = (annotation as Subscribe).topic
        buildPathMap(topic, method)
        val qos = annotation.qos
        val argumentProcessor = SubscriptionArgumentProcessor(pathMap, topic)

        val streamType = method.genericReturnType as ParameterizedType
        val messageType = streamType.getFirstTypeArgument()
        val annotations = method.annotations

        val streamAdapter = streamAdapterResolver.resolve(streamType)
        val messageAdapter = messageAdapterResolver.resolve(messageType, annotations)

        stubMethod = StubMethod.SubscribeWithStream(qos, argumentProcessor, messageAdapter, streamAdapter)
    }

    private fun parseSubscribeAllMethodAnnotations(method: Method) {
        method.requireReturnTypeIsOneOf(Void.TYPE, ParameterizedType::class.java) {
            "Subscribe method must return Void or ParameterizedType: $method"
        }
        val annotations = method.parameterAnnotations[0].filter { it.isParameterAnnotation() }
        require(annotations.size == 1) {
            "A parameter must have one and only one parameter annotation"
        }
        require(method.parameterTypes[0] == Map::class.java) {
            "Parameter should be of Map<String, QoS> type $method"
        }
        val actualTypeArguments =
            (method.genericParameterTypes[0] as ParameterizedType).actualTypeArguments
        require(actualTypeArguments[0] == String::class.java) {
            "Parameter should be of Map<String, QoS> type $method"
        }
        require(actualTypeArguments[1].getRawType() == QoS::class.java) {
            "Parameter should be of Map<String, QoS> type $method"
        }

        if (method.genericReturnType == Void.TYPE) {
            stubMethod = StubMethod.SubscribeAll
        } else {
            method.requireReturnTypeIsResolvable {
                "Method return type must not include a type variable or wildcard: ${method.genericReturnType}"
            }

            val streamType = method.genericReturnType as ParameterizedType
            val messageType = streamType.getFirstTypeArgument()

            val streamAdapter = streamAdapterResolver.resolve(streamType)
            val messageAdapter = messageAdapterResolver.resolve(messageType, method.annotations)

            stubMethod = StubMethod.SubscribeAllWithStream(messageAdapter, streamAdapter)
        }
    }

    private fun parseUnsubscribeMethodAnnotations(method: Method) {
        method.requireReturnTypeIsOneOf(Void.TYPE) {
            "Unsubscribe method must return Void: $method"
        }

        val annotation = method.annotations.first()
        val topics = (annotation as Unsubscribe).topics
        for (topic in topics) {
            buildPathMap(topic, method)
        }
        val argumentProcessor = UnsubscriptionArgumentProcessor(pathMap, topics)
        stubMethod = StubMethod.Unsubscribe(argumentProcessor)
    }

    private fun buildPathMap(topic: String, method: Method) {
        val pathVariablesMap = parsePathVariables(topic)
        val parameterCount = method.parameterAnnotations.size
        require(parameterCount >= pathVariablesMap.size) {
            "Parameter count should be greater than unique path variables: ${method.name}"
        }
        for (parameterIndex in method.parameterAnnotations.indices) {
            val parameterAnnotations = method.parameterAnnotations[parameterIndex]
            val annotations = parameterAnnotations.filter { it.isParameterAnnotation() }
            require(annotations.size == 1) {
                "A parameter must have one and only one parameter annotation. Parameter index: $parameterIndex"
            }

            if (annotations[0] is Path) {
                validatePathName(pathVariablesMap, (annotations[0] as Path).value, parameterIndex)
            }
        }
        pathMap += pathVariablesMap
    }

    private fun parsePathVariables(topic: String): MutableMap<String, Int> {
        val m: Matcher = PARAM_URL_REGEX.matcher(topic)
        val patterns: MutableMap<String, Int> = LinkedHashMap()
        while (m.find()) {
            patterns[m.group(1)] = -1
        }
        return patterns
    }

    private fun validatePathName(pathVariablesMap: MutableMap<String, Int>, name: String, parameterIndex: Int) {
        if (!PARAM_NAME_REGEX.matcher(name).matches()) {
            throw IllegalArgumentException(
                "@Path parameter name must match ${PARAM_URL_REGEX.pattern()}. Found: $name"
            )
        }
        if (pathVariablesMap.contains(name).not()) {
            throw IllegalArgumentException(
                "@Path parameter name must be present in topic: $name"
            )
        }
        if (pathVariablesMap[name] != -1) {
            throw IllegalArgumentException(
                "@Path parameter name must be present only once in parameters: $name"
            )
        }
        pathVariablesMap[name] = parameterIndex
    }

    companion object {
        private const val PARAM = "[a-zA-Z][a-zA-Z0-9_-]*"
        private val PARAM_URL_REGEX = Pattern.compile("\\{($PARAM)\\}")
        private val PARAM_NAME_REGEX = Pattern.compile(PARAM)

        private fun Annotation.isParameterAnnotation(): Boolean {
            return this is Path || this is Data || this is TopicMap || this is Callback
        }

        private fun Annotation.isStubMethodAnnotation(): Boolean {
            return this is Send || this is Receive || this is Subscribe ||
                this is SubscribeMultiple || this is Unsubscribe
        }

        private inline fun Method.requireParameterTypes(vararg types: Class<*>, lazyMessage: () -> Any) {
            require(genericParameterTypes.size == types.size, lazyMessage)
            require(genericParameterTypes.zip(types).all { (t1, t2) -> t2 === t1 || t2.isInstance(t1) }, lazyMessage)
        }

        private inline fun Method.requireReturnTypeIsOneOf(vararg types: Class<*>, lazyMessage: () -> Any) =
            require(types.any { it === genericReturnType || it.isInstance(genericReturnType) }, lazyMessage)

        private inline fun Method.requireReturnTypeIsResolvable(lazyMessage: () -> Any) =
            require(!genericReturnType.hasUnresolvableType(), lazyMessage)

        private fun Method.getDataParameterIndex(): Int {
            var index = -1
            for (parameterIndex in parameterAnnotations.indices) {
                val parameterAnnotations = parameterAnnotations[parameterIndex]
                val annotations = parameterAnnotations.filter { it.isParameterAnnotation() }
                require(annotations.size == 1) {
                    "A parameter must have one and only one parameter annotation: $parameterIndex"
                }
                if (annotations.first() is Data) {
                    if (index == -1) {
                        index = parameterIndex
                        break
                    } else {
                        throw IllegalArgumentException("Multiple parameters found with @Data annotation")
                    }
                }
            }
            if (index == -1) {
                throw IllegalArgumentException("No parameter found with @Data annotation")
            }
            return index
        }

        private fun Method.getCallbackParameterIndex(): Int {
            var index = -1
            for (parameterIndex in parameterAnnotations.indices) {
                val parameterAnnotations = parameterAnnotations[parameterIndex]
                val annotations = parameterAnnotations.filter { it.isParameterAnnotation() }
                require(annotations.size == 1) {
                    "A parameter must have one and only one parameter annotation: $parameterIndex"
                }
                if (annotations.first() is Callback) {
                    if (index == -1) {
                        index = parameterIndex
                        break
                    } else {
                        throw IllegalArgumentException("Multiple parameters found with @Callback annotation")
                    }
                }
            }
            if (index != -1 && parameterTypes[index] != SendMessageCallback::class.java) {
                throw IllegalArgumentException("Parameter annotated with @Callback should be of type SendMessageCallback: ${parameterTypes[index]}")
            }
            return index
        }

        private fun Method.getDataParameterType(index: Int): Type {
            return parameterTypes[index]
        }

        private fun Method.getDataParameterAnnotations(index: Int): Array<Annotation> {
            return parameterAnnotations[index]
        }

        private fun ParameterizedType.getFirstTypeArgument(): Type = getParameterUpperBound(0)
    }
}
