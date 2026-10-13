package com.sugartrack

import android.app.Application
import com.sugartrack.reminders.ReminderNotification

class SugarTrackApplication : Application() {
    override fun onCreate() { super.onCreate(); ReminderNotification.createChannel(this) }
}
