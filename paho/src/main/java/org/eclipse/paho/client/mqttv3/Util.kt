package org.eclipse.paho.client.mqttv3

internal fun <T> readFieldOrNull(instance: Any, fieldType: Class<T>, fieldName: String): T? {
    var c: Class<*> = instance.javaClass
    while (c != Any::class.java) {
        try {
            val field = c.getDeclaredField(fieldName)
            field.isAccessible = true
            val value = field.get(instance)
            return if (!fieldType.isInstance(value)) null else fieldType.cast(value)
        } catch (_: NoSuchFieldException) {
        }

        c = c.superclass
    }

    // Didn't find the field we wanted. As a last gasp attempt,
    // try to find the value on a delegate.
    if (fieldName != "delegate") {
        val delegate = readFieldOrNull(instance, Any::class.java, "delegate")
        if (delegate != null) return readFieldOrNull(delegate, fieldType, fieldName)
    }

    return null
}

internal fun Array<String>.hasIntersection(
    other: Array<String>?,
    comparator: Comparator<in String>
): Boolean {
    if (isEmpty() || other == null || other.isEmpty()) {
        return false
    }
    for (a in this) {
        for (b in other) {
            if (comparator.compare(a, b) == 0) {
                return true
            }
        }
    }
    return false
}

internal fun Array<String>.intersect(
    other: Array<String>,
    comparator: Comparator<in String>
): Array<String> {
    val result = mutableListOf<String>()
    for (a in this) {
        for (b in other) {
            if (comparator.compare(a, b) == 0) {
                result.add(a)
                break
            }
        }
    }
    return result.toTypedArray()
}

internal fun Array<String>.indexOf(value: String, comparator: Comparator<String>): Int =
    indexOfFirst { comparator.compare(it, value) == 0 }

@Suppress("UNCHECKED_CAST")
internal fun Array<String>.concat(value: String): Array<String> {
    val result = copyOf(size + 1)
    result[result.lastIndex] = value
    return result as Array<String>
}
