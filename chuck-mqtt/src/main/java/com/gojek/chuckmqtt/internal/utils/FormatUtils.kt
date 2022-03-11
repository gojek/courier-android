package com.gojek.chuckmqtt.internal.utils

import com.google.gson.JsonParser
import java.util.Locale
import kotlin.math.ln
import kotlin.math.pow

internal fun formatByteCount(bytes: Long, si: Boolean): String {
    val unit = if (si) 1000 else 1024
    if (bytes < unit) return "$bytes B"
    val exp =
        (ln(bytes.toDouble()) / ln(unit.toDouble())).toInt()
    val pre =
        (if (si) "kMGTPE" else "KMGTPE")[exp - 1].toString() + if (si) "" else "i"
    return String.format(
        Locale.US,
        "%.1f %sB",
        bytes / unit.toDouble().pow(exp.toDouble()),
        pre
    )
}

internal fun formatBody(body: String): String {
     return try {
         val je = JsonParser.parseString(body)
         JsonConverter.instance.toJson(je)
     } catch (e: Exception) {
         body
     }
}