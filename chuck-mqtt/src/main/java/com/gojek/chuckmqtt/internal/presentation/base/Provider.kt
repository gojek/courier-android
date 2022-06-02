package com.gojek.chuckmqtt.internal.presentation.base

internal interface Provider<T> {
    /**
     * Provides a fully-constructed and injected instance of `T`.
     *
     * @throws RuntimeException if the injector encounters an error while
     * providing an instance. For example, if an injectable member on
     * `T` throws an exception, the injector may wrap the exception
     * and throw it to the caller of `get()`. Callers should not try
     * to handle such exceptions as the behavior may vary across injector
     * implementations and even different configurations of the same injector.
     */
    fun get(): T
}
