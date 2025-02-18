package com.gojek.courier.messageadapter.kotlinserialization

import com.gojek.courier.Message
import com.gojek.courier.MessageAdapter
import kotlinx.serialization.Serializable
import org.junit.Assert.assertEquals
import org.junit.Test

class KotlinSerializationMessageAdapterTest {
    private val messageAdapter = KotlinSerializationMessageAdapterFactory().create(
        type = TestClass::class.java,
        annotations = emptyArray()
    ) as MessageAdapter<TestClass>

    @Test
    fun `test toMessage`() {
        val testClass = TestClass(100, "test100")
        val testString = """{"id":100,"name":"test100"}"""

        val message = messageAdapter.toMessage(
            topic = "any",
            data = testClass
        )

        assertEquals(testString, String((message as Message.Bytes).value))
    }

    @Test
    fun `test fromMessage`() {
        val testClass = TestClass(100, "test100")
        val testString = """{"id":100,"name":"test100"}"""

        val message = messageAdapter.fromMessage(
            topic = "any",
            message = Message.Bytes(testString.toByteArray())
        )

        assertEquals(testClass, message)
    }
}

@Serializable
data class TestClass(
    val id: Int,
    val name: String
)
