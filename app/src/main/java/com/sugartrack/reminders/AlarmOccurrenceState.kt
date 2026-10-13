package com.sugartrack.reminders

import android.content.Context
import androidx.core.content.edit

object AlarmOccurrenceState {
    const val RINGING = "RINGING"
    const val ACKNOWLEDGED = "ACKNOWLEDGED"
    const val COMPLETED = "COMPLETED"

    private const val PREFS = "alarm_occurrence_state"

    private fun key(profileId: Long, reminderId: Long, occurrenceAt: Long) = "$profileId:$reminderId:$occurrenceAt"

    fun get(context: Context, profileId: Long, reminderId: Long, occurrenceAt: Long): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(key(profileId, reminderId, occurrenceAt), null)

    fun set(context: Context, profileId: Long, reminderId: Long, occurrenceAt: Long, state: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(key(profileId, reminderId, occurrenceAt), state)
            .apply()
    }

    fun clearProfile(context: Context, profileId: Long) {
        val prefix = "$profileId:"
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit {
            prefs.all.keys.filter { it.startsWith(prefix) }.forEach { remove(it) }
        }
    }
}
