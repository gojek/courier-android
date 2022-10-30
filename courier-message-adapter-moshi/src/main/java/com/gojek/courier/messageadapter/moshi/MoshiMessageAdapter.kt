package com.gojek.courier.messageadapter.moshi

import com.gojek.courier.Message
import com.gojek.courier.MessageAdapter
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonQualifier
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.lang.reflect.Type
import okio.ByteString

/**
 * A [message adapter][MessageAdapter] that uses Moshi.
 */
private class MoshiMessageAdapter<T> constructor(
    private val jsonAdapter: JsonAdapter<T>
) : MessageAdapter<T> {

    override fun fromMessage(topic: String, message: Message): T {
        val stringValue = when (message) {
            is Message.Bytes -> {
                val byteString = ByteString.of(message.value, 0, message.value.size)
                // Moshi has no document-level API so the responsibility of BOM skipping falls to whatever is delegating
                // to it. Since it's a UTF-8-only library as well we only honor the UTF-8 BOM.
                if (byteString.startsWith(UTF8_BOM)) {
                    byteString.substring(UTF8_BOM.size()).utf8()
                } else {
                    byteString.utf8()
                }
            }
        }
        return jsonAdapter.fromJson(stringValue)!!
    }

    override fun toMessage(topic: String, data: T): Message {
        val stringValue = jsonAdapter.toJson(data)
        return Message.Bytes(stringValue.toByteArray())
    }

    override fun contentType() = "application/json"

    private companion object {
        private val UTF8_BOM = ByteString.decodeHex("EFBBBF")
    }
}

class MoshiMessageAdapterFactory constructor(
    private val moshi: Moshi = DEFAULT_MOSHI,
    private val config: Config = Config()
) : MessageAdapter.Factory {

    override fun create(type: Type, annotations: Array<Annotation>): MessageAdapter<*> {
        val jsonAnnotations = filterJsonAnnotations(annotations)
        var adapter = moshi.adapter<Any>(type, jsonAnnotations)

        with(config) {
            if (lenient) {
                adapter = adapter.lenient()
            }
            if (serializeNull) {
                adapter = adapter.serializeNulls()
            }
            if (failOnUnknown) {
                adapter = adapter.failOnUnknown()
            }
        }

        return MoshiMessageAdapter(
            adapter
        )
    }

    private fun filterJsonAnnotations(annotations: Array<Annotation>): Set<Annotation> {
        return annotations
            .filter { it.annotationClass.java.isAnnotationPresent(JsonQualifier::class.java) }
            .toSet()
    }

    /**
     * Used to configure `moshi` adapters.
     *
     * @param lenient lenient when reading and writing.
     * @param serializeNull include null values into the serialized JSON.
     * @param failOnUnknown use [JsonAdapter.failOnUnknown] adapters.
     */
    data class Config(
        val lenient: Boolean = false,
        val serializeNull: Boolean = false,
        val failOnUnknown: Boolean = false
    )

    private companion object {
        val DEFAULT_MOSHI = Moshi.Builder()
            .add(KotlinJsonAdapterFactory()).build()
    }
}
