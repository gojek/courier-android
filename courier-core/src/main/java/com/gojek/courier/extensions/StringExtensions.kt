package com.gojek.courier.extensions

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY)
fun String.isWildCardTopic(): Boolean {
    return startsWith("+/") || contains("/+/") || endsWith("/+") || equals("+") ||
        endsWith("/#") || equals("#")
}
