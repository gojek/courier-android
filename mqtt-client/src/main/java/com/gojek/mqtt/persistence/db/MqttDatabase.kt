package com.gojek.mqtt.persistence.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.gojek.mqtt.persistence.dao.IncomingMessagesDao
import com.gojek.mqtt.persistence.dao.PahoMessagesDao
import com.gojek.mqtt.persistence.model.MqttPahoPacket
import com.gojek.mqtt.persistence.model.MqttReceivePacket

@Database(
    entities = [
        MqttReceivePacket::class,
        MqttPahoPacket::class
    ],
    version = MQTT_DB_VERSION
)
internal abstract class MqttDatabase: RoomDatabase() {
    abstract fun incomingMessagesDao(): IncomingMessagesDao

    abstract fun pahoMessagesDao(): PahoMessagesDao
}