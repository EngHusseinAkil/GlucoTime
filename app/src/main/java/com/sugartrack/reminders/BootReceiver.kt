package com.sugartrack.reminders

import android.content.*
import kotlinx.coroutines.*
import com.sugartrack.data.ReminderPreferences

class BootReceiver : BroadcastReceiver() { override fun onReceive(context: Context, intent: Intent) { val result = goAsync(); val app = context.applicationContext; CoroutineScope(Dispatchers.IO).launch { try { ReminderScheduler(app).rescheduleRegular(ReminderPreferences(app).readAll()) } finally { result.finish() } } } }
