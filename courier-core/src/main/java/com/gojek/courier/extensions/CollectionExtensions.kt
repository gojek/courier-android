package com.gojek.courier.extensions

private class ImmutableMap<K, V>(private val inner: Map<K, V>) : Map<K, V> by inner {
    override fun equals(other: Any?): Boolean {
        return inner == other
    }

    override fun hashCode(): Int {
        return inner.hashCode()
    }

    override fun toString(): String {
        return inner.toString()
    }
}
fun <K, V> Map<K, V>.toImmutableMap(): Map<K, V> {
    return if (this is ImmutableMap<K, V>) {
        this
    } else {
        ImmutableMap(this)
    }
}

private class ImmutableSet<E>(private val inner: Set<E>) : Set<E> by inner {
    override fun equals(other: Any?): Boolean {
        return inner == other
    }

    override fun hashCode(): Int {
        return inner.hashCode()
    }

    override fun toString(): String {
        return inner.toString()
    }
}
fun <E> Set<E>.toImmutableSet(): Set<E> {
    return if (this is ImmutableSet<E>) {
        this
    } else {
        ImmutableSet(this)
    }
}