package com.gojek.courier.messageadapter.kotlinserialization

import com.gojek.courier.Message
import com.gojek.courier.MessageAdapter
import java.lang.reflect.Type
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

/**
 * A [message adapter][MessageAdapter] that uses Kotlin Serialization.
 */
private class KotlinSerializationMessageAdapter<T>(
    private val json: Json,
    private val serializer: KSerializer<T>
) : MessageAdapter<T> {

    override fun fromMessage(topic: String, message: Message): T {
        val stringValue = when (message) {
            is Message.Bytes -> String(message.value, Charsets.UTF_8)
        }
        return json.decodeFromString(serializer, stringValue)
    }

    override fun toMessage(topic: String, data: T): Message =
        json.encodeToString(serializer, data)
            .toByteArray(Charsets.UTF_8)
            .let(Message::Bytes)

    override fun contentType() = "application/json"
}

@OptIn(ExperimentalSerializationApi::class)
class KotlinSerializationMessageAdapterFactory(
    private val json: Json = Json.Default
) : MessageAdapter.Factory {

    override fun create(type: Type, annotations: Array<Annotation>): MessageAdapter<*> {
        val serializer = json.serializersModule.serializer(type)
        return KotlinSerializationMessageAdapter(json, serializer)
    }
}
