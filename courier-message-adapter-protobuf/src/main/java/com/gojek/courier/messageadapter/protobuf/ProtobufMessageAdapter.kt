package com.gojek.courier.messageadapter.protobuf

import com.gojek.courier.Message
import com.gojek.courier.MessageAdapter
import com.google.protobuf.ExtensionRegistryLite
import com.google.protobuf.InvalidProtocolBufferException
import com.google.protobuf.MessageLite
import com.google.protobuf.Parser
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Type

/**
 * A [message adapter][MessageAdapter] that uses Protobuf.
 */
private class ProtobufMessageAdapter<T : MessageLite> constructor(
    private val parser: Parser<T>,
    private val registry: ExtensionRegistryLite?
) : MessageAdapter<T> {

    override fun fromMessage(topic: String, message: Message): T {
        val bytesValue = when (message) {
            is Message.Bytes -> message.value
        }
        try {
            return when (registry) {
                null -> parser.parseFrom(bytesValue)
                else -> parser.parseFrom(bytesValue, registry)
            }
        } catch (e: InvalidProtocolBufferException) {
            throw RuntimeException(e) // Despite extending IOException, this is data mismatch.
        }
    }

    override fun toMessage(topic: String, data: T): Message = Message.Bytes(data.toByteArray())

    override fun contentType() = "application/x-protobuf"
}

class ProtobufMessageAdapterFactory(
    private val registry: ExtensionRegistryLite? = null
) : MessageAdapter.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun create(type: Type, annotations: Array<Annotation>): MessageAdapter<*> {
        require(type is Class<*>)
        val clazz = type as Class<*>
        require(MessageLite::class.java.isAssignableFrom(type))

        var parser: Parser<MessageLite>
        try {
            val method = clazz.getDeclaredMethod("parser")
            parser = method.invoke(null) as Parser<MessageLite>
        } catch (e: InvocationTargetException) {
            throw RuntimeException(e.cause)
        } catch (ignored: NoSuchMethodException) {
            // If the method is missing, fall back to original static field for pre-3.0 support.
            try {
                val field = clazz.getDeclaredField("PARSER")
                parser = field.get(null) as Parser<MessageLite>
            } catch (e: NoSuchFieldException) {
                throw ParserNotFoundException(clazz)
            } catch (e: IllegalAccessException) {
                throw ParserNotFoundException(clazz)
            }
        } catch (ignored: IllegalAccessException) {
            try {
                val field = clazz.getDeclaredField("PARSER")
                parser = field.get(null) as Parser<MessageLite>
            } catch (e: NoSuchFieldException) {
                throw ParserNotFoundException(clazz)
            } catch (e: IllegalAccessException) {
                throw ParserNotFoundException(clazz)
            }
        }
        return ProtobufMessageAdapter(parser, registry)
    }
}
