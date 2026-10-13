package com.sugartrack.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Handles acknowledgement from the ongoing alarm notification. */
class AlarmActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        val profileId = intent.getLongExtra(AlarmActivity.PROFILE_ID, -1L)
        val reminderId = intent.getLongExtra(AlarmActivity.REMINDER_ID, -1L)
        val occurrenceAt = intent.getLongExtra(AlarmActivity.OCCURRENCE_AT, -1L)
        if (profileId < 0L || reminderId < 0L || occurrenceAt < 0L) return

        when (intent.action) {
            ACTION_ACKNOWLEDGE -> {
                AlarmRingingService.stopActiveAlarm(app, profileId, reminderId, occurrenceAt, AlarmOccurrenceState.ACKNOWLEDGED)
            }
        }
    }

    companion object {
        const val ACTION_ACKNOWLEDGE = "com.sugartrack.action.ACKNOWLEDGE"
    }
}
