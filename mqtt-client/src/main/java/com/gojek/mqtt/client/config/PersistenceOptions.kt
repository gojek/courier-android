package com.gojek.mqtt.client.config

sealed class PersistenceOptions {
    data class PahoPersistenceOptions(
        val bufferCapacity: Int = OFFLINE_BUFFER_CAPACITY_DEFAULT,
        val isDeleteOldestMessages: Boolean = DELETE_OLDEST_MESSAGES_DEFAULT
    ): PersistenceOptions()
}

private const val OFFLINE_BUFFER_CAPACITY_DEFAULT = 50000
private const val DELETE_OLDEST_MESSAGES_DEFAULT = false