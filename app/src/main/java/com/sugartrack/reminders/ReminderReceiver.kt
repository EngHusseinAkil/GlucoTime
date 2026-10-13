package com.sugartrack.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.sugartrack.data.GlucoseRepository
import com.sugartrack.data.ReminderPreferences
import com.sugartrack.data.SugarTrackDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val result = goAsync()
        val app = context.applicationContext
        val receiverAt = System.currentTimeMillis()
        val id = intent.getLongExtra(REMINDER_ID, -1)
        val profileId = intent.getLongExtra(PROFILE_ID, 1)
        val scheduledAt = intent.getLongExtra(SCHEDULED_AT, receiverAt)
        val occurrence = intent.getLongExtra(OCCURRENCE_AT, scheduledAt)
        Log.d(TAG, "RECEIVER_TRIGGER at=$receiverAt profileId=$profileId reminderId=$id scheduledAt=$scheduledAt occurrence=$occurrence")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                ReminderPreferences(app).read(profileId).firstOrNull { it.id == id }?.takeIf { it.enabled }?.let { reminder ->
                    val repo = GlucoseRepository(SugarTrackDatabase.get(app), profileId)
                    val hasReading = repo.hasReadingSince(occurrence)
                    Log.d(TAG, "READING_CHECK profileId=$profileId reminderId=$id occurrence=$occurrence hasReadingSinceOccurrence=$hasReading")
                    if (!hasReading) {
                        val currentState = AlarmOccurrenceState.get(app, profileId, id, occurrence)
                        val canRing = currentState == null || currentState == AlarmOccurrenceState.RINGING
                        val claimed = canRing && AlarmRingingService.claimOccurrence(profileId, id, occurrence)
                        Log.d(TAG, "STATE_CHECK profileId=$profileId reminderId=$id occurrence=$occurrence state=$currentState canRing=$canRing claimed=$claimed")
                        if (claimed) {
                            AlarmOccurrenceState.set(app, profileId, id, occurrence, AlarmOccurrenceState.RINGING)
                            Log.d(TAG, "STATE_CHANGED profileId=$profileId reminderId=$id occurrence=$occurrence state=RINGING")
                            val serviceIntent = Intent(app, AlarmRingingService::class.java)
                                .putExtra(REMINDER_ID, id)
                                .putExtra(PROFILE_ID, profileId)
                                .putExtra(SCHEDULED_AT, scheduledAt)
                                .putExtra(OCCURRENCE_AT, occurrence)
                                .putExtra(AlarmActivity.LABEL, ReminderNotification.localizedLabel(app, reminder.label))
                            val serviceStart = if (Build.VERSION.SDK_INT >= 26) {
                                app.startForegroundService(serviceIntent)
                            } else {
                                app.startService(serviceIntent)
                            }
                            Log.d(TAG, "SERVICE_START_REQUESTED profileId=$profileId reminderId=$id occurrence=$occurrence result=$serviceStart")
                        }
                        ReminderScheduler(app).schedule(reminder)
                    } else {
                        AlarmOccurrenceState.set(app, profileId, id, occurrence, AlarmOccurrenceState.COMPLETED)
                        Log.d(TAG, "STATE_CHANGED profileId=$profileId reminderId=$id occurrence=$occurrence state=COMPLETED")
                        ReminderNotification.cancel(app, id, occurrence, profileId)
                    }
                }
            } finally {
                result.finish()
            }
        }
    }

    companion object {
        private const val TAG = "ReminderReceiver"
        const val REMINDER_ID = "reminder_id"
        const val PROFILE_ID = "profile_id"
        const val SCHEDULED_AT = "scheduled_at"
        const val OCCURRENCE_AT = "occurrence_at"
    }
}
