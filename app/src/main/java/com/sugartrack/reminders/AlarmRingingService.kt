package com.sugartrack.reminders

import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.core.app.ServiceCompat

class AlarmRingingService : Service() {
    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var activeOccurrenceKey: String? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val reminderId = intent?.getLongExtra(ReminderReceiver.REMINDER_ID, -1) ?: -1
        val profileId = intent?.getLongExtra(ReminderReceiver.PROFILE_ID, 1) ?: 1
        val scheduledAt = intent?.getLongExtra(ReminderReceiver.SCHEDULED_AT, System.currentTimeMillis())
            ?: System.currentTimeMillis()
        val occurrenceAt = intent?.getLongExtra(ReminderReceiver.OCCURRENCE_AT, scheduledAt) ?: scheduledAt
        val label = intent?.getStringExtra(AlarmActivity.LABEL).orEmpty()
        Log.d(TAG, "service start reminderId=$reminderId occurrence=$occurrenceAt")

        if (AlarmOccurrenceState.get(this, profileId, reminderId, occurrenceAt) != AlarmOccurrenceState.RINGING) {
            Log.w(TAG, "service ignored occurrence=$occurrenceAt because it is not ringing")
            stopSelf()
            return START_NOT_STICKY
        }

        ReminderNotification.createChannel(this)
        activeOccurrenceKey = occurrenceKey(profileId, reminderId, occurrenceAt)
        startForeground(
            ReminderNotification.notificationId(profileId, reminderId, occurrenceAt),
            ReminderNotification.build(this, profileId, reminderId, label, scheduledAt, occurrenceAt)
        )
        startRinging()
        return START_NOT_STICKY
    }

    private fun startRinging() {
        if (player == null) {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: return
            player = runCatching {
                MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    setDataSource(this@AlarmRingingService, alarmUri)
                    isLooping = true
                    prepare()
                    start()
                }
            }.getOrNull()
        }

        if (vibrator == null) vibrator = getSystemService(Vibrator::class.java)
        vibrator?.let {
            val pattern = longArrayOf(0, 700, 500)
            if (Build.VERSION.SDK_INT >= 26) {
                it.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(pattern, 0)
            }
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "SERVICE_STOP occurrence=${activeOccurrenceKey.orEmpty()}")
        player?.runCatching { stop() }
        player?.release()
        player = null
        vibrator?.cancel()
        vibrator = null
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        activeOccurrenceKey?.let {
            releaseOccurrence(it)
        }
        activeOccurrenceKey = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "AlarmRingingService"
        private val activeOccurrences = mutableSetOf<String>()

        /** Stops the complete active-alarm experience while preserving the occurrence state. */
        fun stopActiveAlarm(context: android.content.Context, profileId: Long, reminderId: Long, occurrenceAt: Long, state: String? = null) {
            state?.let { AlarmOccurrenceState.set(context, profileId, reminderId, occurrenceAt, it) }
            ReminderNotification.cancel(context, reminderId, occurrenceAt, profileId)
            context.stopService(Intent(context, AlarmRingingService::class.java))
            AlarmActivity.closeIfVisible()
        }

        @Synchronized
        fun occurrenceKey(profileId: Long, reminderId: Long, occurrenceAt: Long): String = "$profileId:$reminderId:$occurrenceAt"

        @Synchronized
        fun claimOccurrence(profileId: Long, reminderId: Long, occurrenceAt: Long): Boolean =
            activeOccurrences.add(occurrenceKey(profileId, reminderId, occurrenceAt))

        @Synchronized
        fun releaseOccurrence(key: String): Boolean = activeOccurrences.remove(key)
    }
}
