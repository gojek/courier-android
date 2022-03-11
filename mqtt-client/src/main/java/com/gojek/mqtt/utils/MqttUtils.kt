package com.gojek.mqtt.utils

import android.content.Context
import android.os.Build
import android.os.Process
import androidx.core.content.PermissionChecker
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.GZIPInputStream

internal class MqttUtils {
    private fun isCompressed(bytes: ByteArray?): Boolean {
        return try {
            if (bytes == null || bytes.size < 2) {
                false
            } else {
                bytes[0] == GZIPInputStream.GZIP_MAGIC.toByte() && bytes[1] == (GZIPInputStream.GZIP_MAGIC shr 8).toByte()
            }
        } catch (e: Exception) {
            false
        }
    }

    @Throws(IOException::class)
    fun uncompressByteArray(bytes: ByteArray?): ByteArray? {
        val DEFAULT_BUFFER_SIZE = 1024 * 4
        if (!isCompressed(bytes)) {
            return bytes
        }
        var bais: ByteArrayInputStream? = null
        var gzis: GZIPInputStream? = null
        var baos: ByteArrayOutputStream? = null
        return try {
            bais = ByteArrayInputStream(bytes)
            gzis = GZIPInputStream(bais)
            baos = ByteArrayOutputStream(DEFAULT_BUFFER_SIZE)
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var n = 0
            while (-1 != gzis.read(buffer).also { n = it }) {
                baos.write(buffer, 0, n)
            }
            gzis.close()
            bais.close()
            val uncompressedByteArray = baos.toByteArray()
            baos.close()
            uncompressedByteArray
        } catch (ioex: IOException) {
            throw ioex
        } finally {
            gzis?.close()
            bais?.close()
            baos?.close()
        }
    }

    val isKitkatOrHigher: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT

    val isMarshmallowOrHigher: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M

    val isLollipopOrHigher: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP

    val isOreoOrHigher: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

    /**
     * Checks that an Iterable is both non-null and non-empty. This method does not check individual elements in the Iterable, it just checks that the Iterable has at least one
     * element.
     *
     * @param argument the argument to validate
     * @return true is argument is empty. false otherwise
     */
    fun <S, T : Iterable<S>?> isEmpty(argument: T): Boolean {
        return argument == null || !argument.iterator().hasNext()
    }

    fun threadFactory(
        name: String,
        daemon: Boolean
    ): ThreadFactory {
        return object : ThreadFactory {
            private val i =
                AtomicInteger(1)

            override fun newThread(runnable: Runnable): Thread {
                val threadCount = i.getAndIncrement()
                val result = Thread(runnable)
                result.name = "$name-$threadCount"
                result.isDaemon = daemon
                result.priority =
                    Process.THREAD_PRIORITY_MORE_FAVORABLE + Process.THREAD_PRIORITY_BACKGROUND
                return result
            }
        }
    }

    fun isPermissionGranted(
        context: Context,
        permission: String
    ): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || PermissionChecker.checkSelfPermission(
            context,
            permission
        ) === PermissionChecker.PERMISSION_GRANTED
    }

    val isAppInForeground: Boolean
        get() = ProcessLifecycleOwner.get().getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)
}