package com.gojek.chuckmqtt.internal

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_ONE_SHOT
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.gojek.chuckmqtt.R
import com.gojek.chuckmqtt.internal.cleanup.ClearDatabaseService
import com.gojek.chuckmqtt.internal.presentation.base.activity.BaseChuckMqttActivity
import com.gojek.courier.utils.extensions.addImmutableFlag

internal class NotificationHelper(private val context: Context) {

    companion object {
        private const val CHANNEL_ID = "mqtt-chuck"
        private const val TRANSACTION_NOTIFICATION_ID = 1990
        private const val INTENT_REQUEST_CODE = 11
    }

    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.mqtt_chuck_notification_category),
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }
    }

    internal fun showNotification(
        transactionNotificationTexts: List<String>,
        totalMessages: Int,
        maxLines: Int
    ) {
        if (!BaseChuckMqttActivity.isInForeground) {
            val builder =
                NotificationCompat.Builder(
                    context,
                    CHANNEL_ID
                )
                    .setContentIntent(
                        PendingIntent.getActivity(
                            context,
                            TRANSACTION_NOTIFICATION_ID,
                            MqttChuck.getLaunchIntent(context),
                            FLAG_UPDATE_CURRENT.addImmutableFlag()
                        )
                    )
                    .setLocalOnly(true)
                    .setSmallIcon(R.drawable.mqtt_chuck_ic_notification)
                    .setColor(ContextCompat.getColor(context, R.color.mqtt_chuck_color_primary))
                    .setContentTitle(context.getString(R.string.mqtt_chuck_mqtt_notification_title))
                    .addAction(createClearAction(ClearDatabaseService.ClearAction.Transaction))
                    .setDefaults(
                        Notification.DEFAULT_SOUND or
                            Notification.DEFAULT_VIBRATE or
                            Notification.DEFAULT_LIGHTS
                    )
            val inboxStyle = NotificationCompat.InboxStyle()
            for ((count, notificationText) in transactionNotificationTexts.withIndex()) {
                if (count < maxLines) {
                    if (count == 0) {
                        builder.setContentText(notificationText)
                    }
                    inboxStyle.addLine(notificationText)
                }
            }
            builder.apply {
                setAutoCancel(true)
                setStyle(inboxStyle)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                builder.setSubText(totalMessages.toString())
            } else {
                builder.setNumber(totalMessages)
            }

            notificationManager.notify(TRANSACTION_NOTIFICATION_ID, builder.build())
        }
    }

    internal fun dismissNotification() {
        notificationManager.cancel(TRANSACTION_NOTIFICATION_ID)
    }

    private fun createClearAction(
        clearAction: ClearDatabaseService.ClearAction
    ): NotificationCompat.Action {
        val clearTitle = context.getString(R.string.mqtt_chuck_clear).toUpperCase()
        val deleteIntent = Intent(context, ClearDatabaseService::class.java).apply {
            putExtra(ClearDatabaseService.EXTRA_ITEM_TO_CLEAR, clearAction)
        }
        val intent = PendingIntent.getService(
            context,
            INTENT_REQUEST_CODE,
            deleteIntent, FLAG_ONE_SHOT.addImmutableFlag()
        )
        return NotificationCompat.Action(R.drawable.mqtt_chuck_ic_delete_white_24dp, clearTitle, intent)
    }
}
