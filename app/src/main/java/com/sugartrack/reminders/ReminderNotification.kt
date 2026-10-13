package com.sugartrack.reminders

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.NotificationCompat
import com.sugartrack.R
import java.text.DateFormat
import java.util.Date

object ReminderNotification {
    const val CHANNEL_ID = "glucose_reminders_alarm_v2"

    fun localizedLabel(context: Context, label: String) = when (label) {
        "Morning" -> context.getString(R.string.morning_label)
        "Noon" -> context.getString(R.string.noon_label)
        "Evening" -> context.getString(R.string.evening_label)
        else -> label
    }

    fun createChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, context.getString(R.string.reminder_channel_name), NotificationManager.IMPORTANCE_HIGH).apply {
                    description = context.getString(R.string.reminder_channel_description)
                    enableVibration(false)
                    setSound(null, null)
                }
            )
        }
    }

    fun build(context: Context, profileId: Long, id: Long, label: String, scheduledAt: Long, occurrenceAt: Long = scheduledAt): Notification {
        Log.d("ReminderNotification", "NOTIFICATION_POST reminderId=$id occurrence=$occurrenceAt")
        createChannel(context)
        val localizedLabel = localizedLabel(context, label)
        val locale = AppCompatDelegate.getApplicationLocales().get(0) ?: java.util.Locale.ENGLISH
        val time = DateFormat.getTimeInstance(DateFormat.SHORT, locale).format(Date(scheduledAt))
        val summary = context.getString(R.string.reminder_summary, localizedLabel, time)
        val fullScreenIntent = Intent(context, AlarmActivity::class.java)
            .putExtra(AlarmActivity.PROFILE_ID, profileId)
            .putExtra(AlarmActivity.REMINDER_ID, id)
            .putExtra(AlarmActivity.LABEL, localizedLabel)
            .putExtra(AlarmActivity.SCHEDULED_AT, scheduledAt)
            .putExtra(AlarmActivity.OCCURRENCE_AT, occurrenceAt)
        val fullScreenPendingIntent = PendingIntent.getActivity(context, notificationId(profileId, id, occurrenceAt), fullScreenIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val acknowledgeIntent = Intent(context, AlarmActionReceiver::class.java)
            .setAction(AlarmActionReceiver.ACTION_ACKNOWLEDGE)
            .putExtra(AlarmActivity.PROFILE_ID, profileId)
            .putExtra(AlarmActivity.REMINDER_ID, id)
            .putExtra(AlarmActivity.LABEL, localizedLabel)
            .putExtra(AlarmActivity.SCHEDULED_AT, scheduledAt)
            .putExtra(AlarmActivity.OCCURRENCE_AT, occurrenceAt)
        val acknowledgePendingIntent = PendingIntent.getBroadcast(context, actionNotificationId(profileId, id, occurrenceAt, 1), acknowledgeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(context.getString(R.string.app_name))
            .setSubText(context.getString(R.string.alarm_notification_title))
            .setContentText(summary)
            .setStyle(NotificationCompat.BigTextStyle().bigText(context.getString(R.string.alarm_notification_body) + "\n" + summary))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setAutoCancel(false)
            .addAction(android.R.drawable.ic_menu_edit, context.getString(R.string.alarm_acknowledge), acknowledgePendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .build()
    }

    fun cancel(context: Context, id: Long, occurrenceAt: Long? = null, profileId: Long = 1) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.cancel(id.hashCode())
        if (occurrenceAt != null) manager.cancel(notificationId(profileId, id, occurrenceAt)) else manager.cancel(id.hashCode())
        context.stopService(Intent(context, AlarmRingingService::class.java))
    }

    fun notificationId(profileId: Long, id: Long, occurrenceAt: Long): Int =
        ((profileId * 31 + id * 17 + occurrenceAt).hashCode() and 0x7fffffff).coerceAtLeast(1)

    private fun actionNotificationId(profileId: Long, id: Long, occurrenceAt: Long, action: Int): Int =
        (notificationId(profileId, id, occurrenceAt) * 10 + action) and 0x7fffffff
}
