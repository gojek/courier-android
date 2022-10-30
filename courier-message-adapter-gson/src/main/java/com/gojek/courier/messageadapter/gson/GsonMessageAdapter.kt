package com.gojek.courier.messageadapter.gson

import com.gojek.courier.Message
import com.gojek.courier.MessageAdapter
import com.google.gson.Gson
import com.google.gson.TypeAdapter
import com.google.gson.reflect.TypeToken
import java.io.OutputStreamWriter
import java.io.StringReader
import java.lang.reflect.Type
import java.nio.charset.StandardCharsets.UTF_8
import okio.Buffer

/**
 * A [message adapter][MessageAdapter] that uses Gson.
 */
private class GsonMessageAdapter<T> constructor(
    private val gson: Gson,
    private val typeAdapter: TypeAdapter<T>
) : MessageAdapter<T> {

    override fun fromMessage(topic: String, message: Message): T {
        val stringValue = when (message) {
            is Message.Bytes -> String(message.value)
        }
        val jsonReader = gson.newJsonReader(StringReader(stringValue))
        return typeAdapter.read(jsonReader)!!
    }

    override fun toMessage(topic: String, data: T): Message {
        val buffer = Buffer()
        val writer = OutputStreamWriter(buffer.outputStream(), UTF_8)
        val jsonWriter = gson.newJsonWriter(writer)
        typeAdapter.write(jsonWriter, data)
        jsonWriter.close()
        val stringValue = buffer.readByteString().utf8()
        return Message.Bytes(stringValue.toByteArray())
    }

    override fun contentType() = "application/json"
}

class GsonMessageAdapterFactory(
    private val gson: Gson = DEFAULT_GSON
) : MessageAdapter.Factory {

    override fun create(type: Type, annotations: Array<Annotation>): MessageAdapter<*> {
        val typeAdapter = gson.getAdapter(TypeToken.get(type))
        return GsonMessageAdapter(gson, typeAdapter)
    }

    private companion object {
        val DEFAULT_GSON = Gson()
    }
}
