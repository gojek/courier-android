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
import com.gojek.mqtt.pingsender.AdaptiveMqttPingSender
import com.gojek.mqtt.pingsender.IPingSenderEvents
import com.gojek.mqtt.pingsender.KeepAlive
import com.gojek.mqtt.pingsender.KeepAliveCalculator
import com.gojek.mqtt.pingsender.NoOpPingSenderEvents
import com.gojek.mqtt.pingsender.keepAliveMillis
import org.eclipse.paho.client.mqttv3.ILogger
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttPingSender
import org.eclipse.paho.client.mqttv3.internal.ClientComms

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
internal class AdaptiveAlarmPingSender(
    private val applicationContext: Context,
    private val pingSenderConfig: AlarmPingSenderConfig,
    private val clock: Clock = Clock(),
    private val buildInfoProvider: BuildInfoProvider = BuildInfoProvider()
) : AdaptiveMqttPingSender {
    private lateinit var comms: ClientComms
    private lateinit var logger: ILogger
    private val alarmReceiver = AlarmReceiver()
    private var pendingIntent: PendingIntent? = null

    private var pingSenderEvents: IPingSenderEvents = NoOpPingSenderEvents()

    private lateinit var keepAliveCalculator: KeepAliveCalculator
    private lateinit var adaptiveKeepAlive: KeepAlive

    @Volatile
    private var hasStarted = false

    override fun setKeepAliveCalculator(keepAliveCalculator: KeepAliveCalculator) {
        this.keepAliveCalculator = keepAliveCalculator
    }

    override fun init(comms: ClientComms, logger: ILogger) {
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
                1,
                Intent(action),
                FLAG_UPDATE_CURRENT.addImmutableFlag()
            )
            schedule(0)
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
            "Unregister alarmreceiver to MqttService" + comms.client.clientId
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

    override fun schedule(ignoredDelay: Long) {
        // Defensinve check as we can get a security exception in start Method .
        if (pendingIntent == null) {
            logger.d(TAG, "Pending intent is null")
            return
        }

        adaptiveKeepAlive = keepAliveCalculator.getUnderTrialKeepAlive()
        val delayInMilliseconds = adaptiveKeepAlive.keepAliveMillis()
        try {
            val nextAlarmInMilliseconds = if (pingSenderConfig.useElapsedRealTimeAlarm) {
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
            val isMqttAllowWhileIdle = pingSenderConfig.isMqttAllowWhileIdle
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
        return with(pingSenderConfig) {
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
            var serverUri = ""
            if (comms.client != null) {
                serverUri = comms.client.serverURI
            }
            pingSenderEvents.mqttPingInitiated(serverUri, adaptiveKeepAlive.keepAliveMillis().fromMillisToSeconds())
            val token: IMqttToken? = comms.sendPingRequest()
            token?.userContext = adaptiveKeepAlive

            // No ping has been sent.
            if (token == null) {
                pingSenderEvents.pingMqttTokenNull(serverUri, adaptiveKeepAlive.keepAliveMillis().fromMillisToSeconds())
                return
            }
            try {
                // Assign new callback to token to execute code after PingResq
                // arrives. Get another wakelock even receiver already has one,
                // release it until ping response returns.
                val pingWakeLockTimeout = pingSenderConfig.pingWakeLockTimeout
                if (pingWakeLockTimeout > 0) {
                    if (wakelock == null) {
                        val pm =
                            applicationContext.getSystemService(Service.POWER_SERVICE) as PowerManager
                        wakelock = pm.newWakeLock(
                            PowerManager.PARTIAL_WAKE_LOCK,
                            wakeLockTag
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
            token.actionCallback = object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken) {
                    logger.d(
                        TAG,
                        "Success. Release lock(" + wakeLockTag + "):" +
                            System.currentTimeMillis()
                    )
                    // Release wakelock when it is done.
                    if (wakelock != null && wakelock!!.isHeld) {
                        wakelock!!.release()
                    }
                    val timeTaken = (clock.nanoTime() - sTime).fromNanosToMillis()
                    pingSenderEvents.pingEventSuccess(serverUri, timeTaken, adaptiveKeepAlive.keepAliveMillis().fromMillisToSeconds())
                    val keepAlive = asyncActionToken.userContext as KeepAlive
                    keepAliveCalculator.onKeepAliveSuccess(keepAlive)
                    schedule(0)
                }

                override fun onFailure(
                    asyncActionToken: IMqttToken,
                    exception: Throwable
                ) {
                    logger.w(
                        TAG,
                        "Failure. Release lock(" + wakeLockTag + "):" +
                            System.currentTimeMillis()
                    )
                    // Release wakelock when it is done.
                    if (wakelock != null && wakelock!!.isHeld) {
                        wakelock!!.release()
                    }
                    val timeTaken = (clock.nanoTime() - sTime).fromNanosToMillis()
                    pingSenderEvents.pingEventFailure(serverUri, timeTaken, exception, adaptiveKeepAlive.keepAliveMillis().fromMillisToSeconds())
                    keepAliveCalculator.onKeepAliveFailure(asyncActionToken.userContext as KeepAlive)
                }
            }
        }
    }

    companion object {
        // Identifier for Intents, log messages, etc..
        private const val TAG = "AlarmPingSender"
        private const val MQTT = "com.gojek.mqtt.adaptive"
        private const val PING_SENDER = "$MQTT.pingSender"

        // Constant for wakelock
        private const val PING_WAKELOCK = "$MQTT.client"
        private const val wakeLockTag = PING_WAKELOCK
    }
}
