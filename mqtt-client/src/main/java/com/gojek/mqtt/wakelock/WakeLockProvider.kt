package com.gojek.mqtt.wakelock

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import com.gojek.courier.logging.ILogger

internal class WakeLockProvider(
    private val context: Context,
    private val logger: ILogger
) {
    private var wakelock: WakeLock? = null

    /**
     * This function will be used when wakeLock is taken during connect as
     *
     *
     * timeout : seconds
     */
    @SuppressLint("InvalidWakeLockTag")
    fun acquireWakeLock(timeout: Int) {
        if (timeout > 0) {
            if (wakelock == null) {
                val pm = context.getSystemService(Service.POWER_SERVICE) as PowerManager
                wakelock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG)
                wakelock!!.setReferenceCounted(false)
            }
            wakelock!!.acquire(timeout * 1000.toLong())
        }
        logger.d(TAG, "Wakelock Acquired")
    }

    fun releaseWakeLock() {
        if (wakelock != null && wakelock!!.isHeld) {
            wakelock!!.release()
            logger.d(TAG, "Wakelock Released")
        }
    }

    companion object {
        const val TAG = "WakeLockProvider"
        const val WAKE_LOCK_TAG = "MQTTWLock"
    }
}
