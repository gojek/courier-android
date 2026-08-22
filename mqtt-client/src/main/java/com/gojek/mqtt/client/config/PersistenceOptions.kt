package com.gojek.mqtt.client.config

data class PersistenceOptions(
    val shouldUseMemoryPersistence: Boolean = false,
    val memoryPersistenceCapacity: Int = 100,
    val bufferCapacity: Int = OFFLINE_BUFFER_CAPACITY_DEFAULT,
    val isPersistBuffer: Boolean = PERSIST_BUFFER_DEFAULT,
    val isDeleteOldestMessages: Boolean = DELETE_OLDEST_MESSAGES_DEFAULT
)

private const val OFFLINE_BUFFER_CAPACITY_DEFAULT = 50000
private const val PERSIST_BUFFER_DEFAULT = true
private const val DELETE_OLDEST_MESSAGES_DEFAULT = false
