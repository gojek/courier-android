package com.gojek.alarm.pingsender

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.AlarmManager.ELAPSED_REALTIME
import android.app.AlarmManager.ELAPSED_REALTIME_WAKEUP
import android.app.AlarmManager.RTC
import android.app.AlarmManager.RTC_WAKEUP
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import android.os.SystemClock
import com.gojek.courier.extensions.fromMillisToSeconds
import com.gojek.courier.extensions.fromNanosToMillis
import com.gojek.courier.utils.BuildInfoProvider
import com.gojek.courier.utils.Clock
import com.gojek.courier.utils.extensions.addImmutableFlag
import com.gojek.mqtt.pingsender.IPingSenderEvents
import com.gojek.mqtt.pingsender.MqttPingSender
import com.gojek.mqtt.pingsender.NoOpPingSenderEvents
import org.eclipse.paho.client.mqtt.ILogger
import org.eclipse.paho.client.mqtt.internal.IClientComms
import org.eclipse.paho.client.mqtt.internal.PingActivityCallBack

/**
 * Default ping sender implementation on Android. It is based on AlarmManager.
 *
 *
 *
 * This class implements the [MqttPingSender] pinger interface
 * allowing applications to send ping packet to server every keep alive interval.
 *
 *
 * @see MqttPingSender
 */
internal class AlarmPingSender(
    private val applicationContext: Context,
    private val alarmPingSenderConfig: AlarmPingSenderConfig,
    private val clock: Clock = Clock(),
    private val buildInfoProvider: BuildInfoProvider = BuildInfoProvider()
) : MqttPingSender {
    private lateinit var comms: IClientComms
    private lateinit var logger: ILogger
    private val alarmReceiver = AlarmReceiver()
    private var pendingIntent: PendingIntent? = null

    private var pingSenderEvents: IPingSenderEvents = NoOpPingSenderEvents()

    @Volatile
    private var hasStarted = false

    override fun init(comms: IClientComms, logger: ILogger) {
        this.comms = comms
        this.logger = logger
    }

    override fun start() {
        val action = PING_SENDER
        logger.d(TAG, "Register alarmreceiver to MqttService$action")
        /*
          Catching Security exception and logging it to analytics to it.
         */try {
            applicationContext.registerReceiver(alarmReceiver, IntentFilter(action))
            pendingIntent = PendingIntent.getBroadcast(
                applicationContext,
                0,
                Intent(action),
                FLAG_UPDATE_CURRENT.addImmutableFlag()
            )
            schedule(comms.keepAlive)
            hasStarted = true
        } catch (e: SecurityException) {
            logger.e(
                TAG,
                "Security Exception while registering Alarm Broadcast Receiver"
            )
            pingSenderEvents.exceptionInStart(e)
        }
    }

    override fun stop() {
        try {
            // Cancel Alarm.
            val alarmManager =
                applicationContext.getSystemService(Service.ALARM_SERVICE) as AlarmManager

            // pending intent can be null if we get a security exception in onstart-->defensive check
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
            }
        } catch (ex: Exception) {
            logger.d(TAG, "Unregister alarmreceiver to MqttService$ex")
        }
        logger.d(
            TAG,
            "Unregister alarmreceiver to MqttService" + comms.clientId
        )
        if (hasStarted) {
            hasStarted = false
            try {
                applicationContext.unregisterReceiver(alarmReceiver)
            } catch (e: IllegalArgumentException) {
                // Ignore unregister errors.
            }
        }
    }

    override fun schedule(delayInMilliseconds: Long) {
        // Defensinve check as we can get a security exception in start Method .
        if (pendingIntent == null) {
            logger.d(TAG, "Pending intent is null")
            return
        }
        try {
            val nextAlarmInMilliseconds = if (alarmPingSenderConfig.useElapsedRealTimeAlarm) {
                SystemClock.elapsedRealtime() + delayInMilliseconds
            } else {
                System.currentTimeMillis() + delayInMilliseconds
            }

            logger.d(
                TAG,
                "Schedule next alarm at $nextAlarmInMilliseconds"
            )
            val alarmManager =
                applicationContext.getSystemService(Service.ALARM_SERVICE) as AlarmManager
            val alarmType = getAlarmType()
            val isMqttAllowWhileIdle = alarmPingSenderConfig.isMqttAllowWhileIdle
            if (isMqttAllowWhileIdle && buildInfoProvider.isMarshmallowOrHigher) {
                alarmManager.setExactAndAllowWhileIdle(
                    alarmType,
                    nextAlarmInMilliseconds,
                    pendingIntent
                )
            } else if (buildInfoProvider.isKitkatOrHigher) {
                alarmManager.setExact(
                    alarmType,
                    nextAlarmInMilliseconds,
                    pendingIntent
                )
            } else {
                alarmManager[alarmType, nextAlarmInMilliseconds] =
                    pendingIntent
            }
            pingSenderEvents.mqttPingScheduled(delayInMilliseconds.fromMillisToSeconds(), comms.keepAlive.fromMillisToSeconds())
        } catch (ex: Exception) {
            logger.d(
                TAG,
                " Exception while sceduling Alaram due to $ex"
            )
            pingSenderEvents.exceptionInStart(ex)
        }
    }

    override fun setPingEventHandler(pingSenderEvents: IPingSenderEvents) {
        this.pingSenderEvents = pingSenderEvents
    }

    private fun getAlarmType(): Int {
        return with(alarmPingSenderConfig) {
            when {
                isMqttPingWakeUp && useElapsedRealTimeAlarm -> ELAPSED_REALTIME_WAKEUP
                isMqttPingWakeUp && useElapsedRealTimeAlarm.not() -> RTC_WAKEUP
                isMqttPingWakeUp.not() && useElapsedRealTimeAlarm -> ELAPSED_REALTIME
                isMqttPingWakeUp.not() && useElapsedRealTimeAlarm.not() -> RTC
                else -> RTC_WAKEUP
            }
        }
    }

    /*
     * This class sends PingReq packet to MQTT broker
     */
    private inner class AlarmReceiver : BroadcastReceiver() {
        private var wakelock: WakeLock? = null

        @SuppressLint("InvalidWakeLockTag")
        override fun onReceive(
            context: Context,
            intent: Intent
        ) {
            // According to the docs, "Alarm Manager holds a CPU wake lock as
            // long as the alarm receiver's onReceive() method is executing.
            // This guarantees that the phone will not sleep until you have
            // finished handling the broadcast.", but this class still get
            // a wake lock to wait for ping finished.
            logger.d(
                TAG,
                "Check time :" + System.currentTimeMillis()
            )
            logger.setAppKillTime(System.currentTimeMillis())
            val serverUri = comms.serverUri ?: ""
            pingSenderEvents.mqttPingInitiated(serverUri, comms.keepAlive.fromMillisToSeconds())

            try {
                // Assign new callback to token to execute code after PingResq
                // arrives. Get another wakelock even receiver already has one,
                // release it until ping response returns.
                val pingWakeLockTimeout = alarmPingSenderConfig.pingWakeLockTimeout
                if (pingWakeLockTimeout > 0) {
                    if (wakelock == null) {
                        val pm =
                            applicationContext.getSystemService(Service.POWER_SERVICE) as PowerManager
                        wakelock = pm.newWakeLock(
                            PowerManager.PARTIAL_WAKE_LOCK,
                            Companion.wakeLockTag
                        )
                        wakelock!!.setReferenceCounted(false)
                        wakelock!!.acquire(pingWakeLockTimeout.toLong())
                    } else {
                        wakelock!!.acquire(pingWakeLockTimeout.toLong())
                    }
                }
            } catch (ex: Exception) {
                logger.d(
                    TAG,
                    "Exception while AlaramBroadcast receive$ex"
                )
            }
            val sTime = clock.nanoTime()
            comms.checkActivityWithCallback(object: PingActivityCallBack {
                override fun onPingMqttTokenNull() {
                    logger.d(TAG, "Mqtt Ping Token null")
                    pingSenderEvents.pingMqttTokenNull(serverUri, comms.keepAlive.fromMillisToSeconds())
                }

                override fun onSuccess() {
                    logger.d(
                        TAG,
                        "Success. Release lock(" + Companion.wakeLockTag + "):" +
                            System.currentTimeMillis()
                    )
                    // Release wakelock when it is done.
                    if (wakelock != null && wakelock!!.isHeld) {
                        wakelock!!.release()
                    }
                    val timeTaken = (clock.nanoTime() - sTime).fromNanosToMillis()
                    pingSenderEvents.pingEventSuccess(serverUri, timeTaken, comms.keepAlive.fromMillisToSeconds())
                }

                override fun onFailure(throwable: Throwable) {
                    logger.w(
                        TAG,
                        "Failure. Release lock(" + Companion.wakeLockTag + "):" +
                            System.currentTimeMillis()
                    )
                    // Release wakelock when it is done.
                    if (wakelock != null && wakelock!!.isHeld) {
                        wakelock!!.release()
                    }
                    val timeTaken = (clock.nanoTime() - sTime).fromNanosToMillis()
                    pingSenderEvents.pingEventFailure(serverUri, timeTaken, throwable, comms.keepAlive.fromMillisToSeconds())
                }
            })
        }
    }

    companion object {
        // Identifier for Intents, log messages, etc..
        private const val TAG = "AlarmPingSender"
        private const val MQTT = "com.gojek.mqtt"
        private const val PING_SENDER = "$MQTT.pingSender"

        // Constant for wakelock
        private const val PING_WAKELOCK = "$MQTT.client"
        private const val wakeLockTag = PING_WAKELOCK
    }
}
