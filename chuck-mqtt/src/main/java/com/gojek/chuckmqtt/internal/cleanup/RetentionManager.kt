package com.gojek.chuckmqtt.internal.cleanup

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import com.gojek.chuckmqtt.external.Period
import com.gojek.chuckmqtt.internal.MqttChuck
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

/**
 * Class responsible of holding the logic for the retention of your HTTP transactions
 * and your throwable. You can customize how long data should be stored here.
 * @param context An Android Context
 * @param retentionPeriod A [Period] to specify the retention of data. Default 1 week.
 */
@Suppress("MagicNumber")
internal class RetentionManager @JvmOverloads constructor(
    context: Context,
    retentionPeriod: Period = Period.ONE_WEEK
) {

    // The actual retention period in milliseconds (default to ONE_WEEK)
    private val period: Long = toMillis(retentionPeriod)

    // How often the cleanup should happen
    private val cleanupFrequency: Long
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, 0)

    init {
        cleanupFrequency = if (retentionPeriod == Period.ONE_HOUR) {
            TimeUnit.MINUTES.toMillis(30)
        } else {
            TimeUnit.HOURS.toMillis(2)
        }
    }

    /**
     * Call this function to check and eventually trigger a cleanup.
     * Please note that this method is not forcing a cleanup.
     */
    @Synchronized
    internal fun doMaintenance() {
        if (period > 0) {
            val now = System.currentTimeMillis()
            if (isCleanupDue(now)) {
                deleteSince(getThreshold(now))
                updateLastCleanup(now)
            }
        }
    }

    private fun isCleanupDue(now: Long) = now - getLastCleanup(now) > cleanupFrequency

    private fun getLastCleanup(now: Long): Long {
        if (lastCleanup == 0L) {
            val storedLastCleanupTime = prefs.getLong(KEY_LAST_CLEANUP, 0L)
            if (storedLastCleanupTime == 0L) {
                updateLastCleanup(now)
            } else {
                lastCleanup = storedLastCleanupTime
            }
        }
        return lastCleanup
    }

    private fun updateLastCleanup(time: Long) {
        lastCleanup = time
        prefs.edit().putLong(KEY_LAST_CLEANUP, time).apply()
    }

    @SuppressLint("CheckResult")
    private fun deleteSince(threshold: Long) {
        MqttChuck.mqttChuckUseCase().deleteOldTransactions(threshold)
            .subscribeOn(Schedulers.computation())
            .subscribe({}, {})
    }

    private fun getThreshold(now: Long) = if (period == 0L) now else now - period

    private fun toMillis(period: Period): Long {
        return when (period) {
            Period.ONE_HOUR -> TimeUnit.HOURS.toMillis(1)
            Period.ONE_DAY -> TimeUnit.DAYS.toMillis(1)
            Period.ONE_WEEK -> TimeUnit.DAYS.toMillis(7)
            Period.FOREVER -> 0
        }
    }

    companion object {
        private const val PREFS_NAME = "mqtt-chuck_pref"
        private const val KEY_LAST_CLEANUP = "last_cleanup"
        private var lastCleanup: Long = 0
    }
}
