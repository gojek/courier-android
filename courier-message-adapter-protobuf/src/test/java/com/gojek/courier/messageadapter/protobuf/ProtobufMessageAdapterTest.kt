package com.gojek.courier.messageadapter.protobuf

import com.gojek.courier.Message
import com.gojek.courier.MessageAdapter
import com.gojek.courier.messageadapter.protobuf.UserProtos.User
import java.util.Base64
import kotlin.test.assertEquals
import org.junit.Test

class ProtobufMessageAdapterTest {
    private val protobufMessageAdapterFactory = ProtobufMessageAdapterFactory()

    private val protobufMessageAdapter = protobufMessageAdapterFactory.create(
        User::class.java,
        emptyArray()
    ) as MessageAdapter<User>

    @Test
    fun serialise() {
        val user = User.newBuilder().setId(123).setName("Test").build()
        val encodedString = "CgRUZXN0EHs="
        val userMessage = protobufMessageAdapter.toMessage(user)
        val userString = String(Base64.getEncoder().encode((userMessage as Message.Bytes).value))
        assertEquals(userString, encodedString)
    }

    @Test
    fun deserialise() {
        val encodedString = "CgRUZXN0EHs="
        val byteArray = Base64.getDecoder().decode(encodedString)
        val user = protobufMessageAdapter.fromMessage(Message.Bytes(byteArray))
        assertEquals(user.id, 123)
        assertEquals(user.name, "Test")
    }

    @Test(expected = RuntimeException::class)
    fun deserialiseEmpty() {
        val byteArray = ByteArray(0)
        protobufMessageAdapter.fromMessage(Message.Bytes(byteArray))
    }
}
