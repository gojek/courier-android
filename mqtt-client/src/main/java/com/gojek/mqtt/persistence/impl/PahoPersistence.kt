package com.gojek.mqtt.persistence.impl

import android.content.Context
import androidx.room.Room
import com.gojek.mqtt.persistence.IMqttReceivePersistence
import com.gojek.mqtt.persistence.dao.IncomingMessagesDao
import com.gojek.mqtt.persistence.dao.PahoMessagesDao
import com.gojek.mqtt.persistence.db.MqttDatabase
import com.gojek.mqtt.persistence.model.MqttPahoPacket
import com.gojek.mqtt.persistence.model.MqttReceivePacket
import org.eclipse.paho.client.mqttv3.MqttClientPersistence
import org.eclipse.paho.client.mqttv3.MqttPersistable
import org.eclipse.paho.client.mqttv3.internal.MqttPersistentData
import java.util.Enumeration
import java.util.Collections

internal class PahoPersistence(private val context: Context) :
    MqttClientPersistence, IMqttReceivePersistence {
    private lateinit var database: MqttDatabase
    private lateinit var incomingMessagesDao: IncomingMessagesDao
    private lateinit var pahoMessagesDao: PahoMessagesDao

    override fun open(clientId: String, serverURI: String) {
        this.database = Room.databaseBuilder(
            context.applicationContext,
            MqttDatabase::class.java,
            "$clientId:mqtt-db"
        ).fallbackToDestructiveMigration()
            .build()
        this.incomingMessagesDao = database.incomingMessagesDao()
        this.pahoMessagesDao = database.pahoMessagesDao()
    }

    override fun close() = Unit

    override fun put(key: String, persistable: MqttPersistable) {
        pahoMessagesDao.insertMessage(
            MqttPahoPacket(
                key = key,
                headerBytes = persistable.headerBytes,
                headerLength = persistable.headerLength,
                headerOffset = persistable.headerOffset,
                payloadBytes = persistable.payloadBytes,
                payloadLength = persistable.payloadLength,
                payloadOffset = persistable.payloadOffset
            )
        )
    }

    override fun get(key: String): MqttPersistable {
        val message = pahoMessagesDao.getMessageByKey(key)
        return MqttPersistentData(
            message.key,
            message.headerBytes,
            message.headerOffset,
            message.headerLength,
            message.payloadBytes,
            message.payloadOffset,
            message.payloadLength
        )
    }

    override fun remove(key: String) {
        pahoMessagesDao.deleteMessageByKey(key)
    }

    override fun keys(): Enumeration<*> {
        return Collections.enumeration(pahoMessagesDao.getAllKeys())
    }

    override fun clear() {
        pahoMessagesDao.clearAllMessages()
    }

    override fun containsKey(key: String): Boolean {
        return pahoMessagesDao.containsMessage(key) > 1
    }
    
    override fun addReceivedMessage(mqttPacket: MqttReceivePacket) {
        incomingMessagesDao.addMessage(mqttPacket)
    }

    override fun getAllIncomingMessages(): List<MqttReceivePacket> {
        return incomingMessagesDao.getAllMessages()
    }

    override fun removeReceivedMessage(mqttPacket: MqttReceivePacket) {
        incomingMessagesDao.removeMessage(mqttPacket)
    }

    fun clearAll() {
        if (::pahoMessagesDao.isInitialized) {
            pahoMessagesDao.clearAllMessages()
        }
        if (::incomingMessagesDao.isInitialized) {
            incomingMessagesDao.clearAllMessages()
        }
    }
}