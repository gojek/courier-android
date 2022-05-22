package com.gojek.chuckmqtt.internal.data.local.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.gojek.chuckmqtt.internal.data.local.entity.MqttTransaction

@Database(entities = [MqttTransaction::class], version = 1, exportSchema = false)
internal abstract class MqttChuckDatabase : RoomDatabase() {

    abstract fun mqttTransactionDao(): MqttTransactionDao

    companion object {
        private val DB_NAME = "mqtt_chuck"

        fun create(context: Context): MqttChuckDatabase {
            return Room.databaseBuilder(context, MqttChuckDatabase::class.java, DB_NAME)
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
