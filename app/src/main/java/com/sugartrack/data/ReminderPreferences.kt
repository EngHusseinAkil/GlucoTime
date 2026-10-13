package com.sugartrack.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

private val Context.reminderDataStore by preferencesDataStore("reminder_settings")

class ReminderPreferences(private val context: Context) {
    private val remindersKey = stringPreferencesKey("reminders_v2")
    private val morningEnabled = booleanPreferencesKey("morning_enabled")
    private val morningHour = intPreferencesKey("morning_hour")
    private val morningMinute = intPreferencesKey("morning_minute")
    private val eveningEnabled = booleanPreferencesKey("evening_enabled")
    private val eveningHour = intPreferencesKey("evening_hour")
    private val eveningMinute = intPreferencesKey("evening_minute")
    private val frequencyDays = intPreferencesKey("frequency_days")

    suspend fun read(profileId: Long = 1): List<Reminder> {
        val p = context.reminderDataStore.data.first()
        p[remindersKey]?.let { return decode(JSONArray(it)).filter { r -> r.profileId == profileId } }
        val now = System.currentTimeMillis()
        val days = (p[frequencyDays] ?: 1).coerceAtLeast(1)
        val migrated = buildList {
            if (p[morningEnabled] == true) add(Reminder(1, true, "Morning", p[morningHour] ?: 7, p[morningMinute] ?: 30, intervalDays = days, createdAt = now, updatedAt = now, profileId = profileId))
            if (p[eveningEnabled] == true) add(Reminder(2, true, "Evening", p[eveningHour] ?: 20, p[eveningMinute] ?: 15, intervalDays = days, createdAt = now, updatedAt = now, profileId = profileId))
        }
        if (migrated.isNotEmpty()) write(profileId, migrated)
        return migrated
    }

    suspend fun readAll(): List<Reminder> {
        val p = context.reminderDataStore.data.first()
        return p[remindersKey]?.let { decode(JSONArray(it)) } ?: read(1)
    }

    suspend fun write(profileId: Long, reminders: List<Reminder>) {
        context.reminderDataStore.edit { prefs ->
            val all = persisted(prefs).filter { it.profileId != profileId } + reminders.map { it.copy(profileId = profileId) }
            prefs[remindersKey] = JSONArray().apply { all.forEach { put(encode(it)) } }.toString()
        }
    }
    suspend fun deleteProfile(profileId: Long) {
        write(profileId, emptyList())
    }

    private fun persisted(prefs: Preferences): List<Reminder> = prefs[remindersKey]?.let { decode(JSONArray(it)) } ?: emptyList()
    private fun encode(r: Reminder) = JSONObject().apply { put("id", r.id); put("profileId", r.profileId); put("enabled", r.enabled); put("label", r.label); put("hour", r.hour); put("minute", r.minute); put("mode", r.repeatMode.name); put("weekdays", JSONArray(r.selectedWeekdays.toList())); put("intervalDays", r.intervalDays); put("createdAt", r.createdAt); put("updatedAt", r.updatedAt) }
    private fun decode(a: JSONArray) = (0 until a.length()).mapNotNull { i -> runCatching { val o = a.getJSONObject(i); val w = o.optJSONArray("weekdays") ?: JSONArray(); val interval = o.optInt("intervalDays", 1).coerceAtLeast(1); val mode = runCatching { RepeatMode.valueOf(o.optString("mode")) }.getOrDefault(RepeatMode.EVERY_DAYS); val normalized = if (mode == RepeatMode.EVERY_DAYS && interval > 1) RepeatMode.EVERY_X_DAYS else mode; Reminder(o.getLong("id"), o.optBoolean("enabled", true), o.optString("label", "Reminder"), o.optInt("hour"), o.optInt("minute"), normalized, (0 until w.length()).map { w.getInt(it) }.toSet(), interval, o.optLong("createdAt"), o.optLong("updatedAt"), o.optLong("profileId", 1)) }.getOrNull() }
}
