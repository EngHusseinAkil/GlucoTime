package com.sugartrack.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.sugartrack.data.Reminder
import com.sugartrack.data.RepeatMode
import java.util.Calendar

object ReminderScheduleCalculator {
    fun nextTrigger(r: Reminder, from: Long = System.currentTimeMillis()): Long {
        val base = Calendar.getInstance().apply {
            timeInMillis = from
            set(Calendar.HOUR_OF_DAY, r.hour)
            set(Calendar.MINUTE, r.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (r.repeatMode == RepeatMode.EVERY_DAYS || r.repeatMode == RepeatMode.EVERY_X_DAYS) {
            if (base.timeInMillis <= from) {
                val interval = if (r.repeatMode == RepeatMode.EVERY_DAYS && r.intervalDays <= 1) 1 else r.intervalDays.coerceAtLeast(2)
                base.add(Calendar.DAY_OF_YEAR, interval)
            }
            return base.timeInMillis
        }
        if (r.selectedWeekdays.isEmpty()) return Long.MAX_VALUE
        for (offset in 0..7) {
            val candidate = (base.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, offset) }
            if (candidate.timeInMillis > from && candidate.get(Calendar.DAY_OF_WEEK) in r.selectedWeekdays) return candidate.timeInMillis
        }
        return Long.MAX_VALUE
    }
}

class ReminderScheduler(private val context: Context) {
    private val alarms = context.getSystemService(AlarmManager::class.java)

    fun exactAlarmAccessAvailable(): Boolean = Build.VERSION.SDK_INT < 31 || alarms.canScheduleExactAlarms()

    private fun pending(profileId: Long, id: Long, scheduledAt: Long = 0, occurrenceAt: Long = 0): PendingIntent {
        val request = (((id * 31 + profileId).toInt()) and 0x7fffffff).coerceAtLeast(1)
        Log.d(TAG, "PENDING_INTENT requestCode=$request profileId=$profileId reminderId=$id occurrence=$occurrenceAt scheduledAt=$scheduledAt")
        val intent = Intent(context, ReminderReceiver::class.java)
            .putExtra(ReminderReceiver.PROFILE_ID, profileId)
            .putExtra(ReminderReceiver.REMINDER_ID, id)
            .putExtra(ReminderReceiver.SCHEDULED_AT, scheduledAt)
            .putExtra(ReminderReceiver.OCCURRENCE_AT, occurrenceAt)
        return PendingIntent.getBroadcast(context, request, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    fun cancel(id: Long, profileId: Long = 1) {
        alarms.cancel(pending(profileId, id))
    }

    fun rescheduleAll(reminders: List<Reminder>) {
        reminders.forEach { cancel(it.id, it.profileId) }
        reminders.filter { it.enabled }.forEach { schedule(it) }
    }

    /** Refreshes the repeating alarms without changing their recurrence. */
    fun rescheduleRegular(reminders: List<Reminder>) {
        reminders.forEach {
            alarms.cancel(pending(it.profileId, it.id))
            if (it.enabled) schedule(it)
        }
    }

    fun schedule(r: Reminder, from: Long = System.currentTimeMillis()) {
        val at = ReminderScheduleCalculator.nextTrigger(r, from)
        if (at != Long.MAX_VALUE) set(at, pending(r.profileId, r.id, scheduledAt = at, occurrenceAt = at))
    }

    private fun set(at: Long, pi: PendingIntent) {
        val exactAllowed = Build.VERSION.SDK_INT < 31 || alarms.canScheduleExactAlarms()
        try {
            when {
                Build.VERSION.SDK_INT >= 31 && exactAllowed -> {
                    Log.d(TAG, "ALARM_SET exact=true at=$at")
                    alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
                }
                Build.VERSION.SDK_INT >= 23 -> {
                    Log.w(TAG, "ALARM_SET exact=false exactAlarmAccess=$exactAllowed at=$at; using inexact idle-allowed fallback")
                    alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
                }
                else -> {
                    Log.w(TAG, "ALARM_SET exact=false api=${Build.VERSION.SDK_INT} at=$at; using legacy fallback")
                    alarms.set(AlarmManager.RTC_WAKEUP, at, pi)
                }
            }
        } catch (error: SecurityException) {
            // Exact-alarm access can be revoked between the capability check and the call.
            Log.w(TAG, "ALARM_SET exact request rejected at=$at; using fallback", error)
            if (Build.VERSION.SDK_INT >= 23) alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            else alarms.set(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }

    companion object {
        private const val TAG = "ReminderScheduler"
    }
}
