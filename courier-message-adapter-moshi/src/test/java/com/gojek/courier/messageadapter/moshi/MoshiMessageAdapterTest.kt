package com.gojek.courier.messageadapter.moshi

import com.gojek.courier.Message
import com.gojek.courier.MessageAdapter
import com.squareup.moshi.FromJson
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import kotlin.test.assertEquals
import org.junit.Test

class MoshiMessageAdapterTest {

    val moshiMessageAdapterFactory = MoshiMessageAdapterFactory(
        Moshi.Builder().add(TestClassAdapter()).build()
    )
    val moshiMessageAdapter = moshiMessageAdapterFactory.create(
        TestClass::class.java,
        emptyArray()
    ) as MessageAdapter<TestClass>

    @Test
    fun `test toMessage`() {
        val testClass = TestClass(100, "test100")
        val testString = """{"id":100,"name":"test100"}"""

        val message = moshiMessageAdapter.toMessage(topic = "any", data = testClass)

        assertEquals(testString, String((message as Message.Bytes).value))
    }

    @Test
    fun `test fromMessage`() {
        val testClass = TestClass(100, "test100")
        val testString = """{"id":100,"name":"test100"}"""

        val message = moshiMessageAdapter.fromMessage(
            topic = "any",
            message = Message.Bytes(testString.toByteArray())
        )

        assertEquals(testClass, message)
    }
}

@JsonClass(generateAdapter = true)
data class TestClass(
    @Json(name = "id") val id: Int,
    @Json(name = "name") val name: String
)

class TestClassAdapter {
    @ToJson
    fun toJson(jsonWriter: JsonWriter, testClass: TestClass) {
        jsonWriter.beginObject()
        jsonWriter.name("id").value(testClass.id)
        jsonWriter.name("name").value(testClass.name)
        jsonWriter.endObject()
    }

    @FromJson
    fun fromJson(reader: JsonReader): TestClass {
        reader.beginObject()
        var id = 0
        var name = ""
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "id" -> id = reader.nextInt()
                "name" -> name = reader.nextString()
            }
        }
        reader.endObject()
        return TestClass(id, name)
    }
}
