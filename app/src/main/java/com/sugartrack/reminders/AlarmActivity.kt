package com.sugartrack.reminders

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.sugartrack.R
import com.sugartrack.ui.theme.GlucoTimeTheme
import java.lang.ref.WeakReference
import java.text.DateFormat
import java.util.Date

class AlarmActivity : ComponentActivity() {
    private var reminderId: Long = -1

    override fun onStart() {
        super.onStart()
        activeReference = WeakReference(this)
    }

    override fun onStop() {
        if (activeReference?.get() === this) activeReference = null
        super.onStop()
    }

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        reminderId = intent.getLongExtra(REMINDER_ID, -1)
        val profileId = intent.getLongExtra(PROFILE_ID, 1)
        
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val rawLabel = intent.getStringExtra(LABEL).orEmpty()
        val at = intent.getLongExtra(SCHEDULED_AT, System.currentTimeMillis())
        val occurrence = intent.getLongExtra(OCCURRENCE_AT, at)
        Log.d(TAG, "ACTIVITY_OPEN profileId=$profileId reminderId=$reminderId occurrence=$occurrence state=${AlarmOccurrenceState.get(this, profileId, reminderId, occurrence)}")
        
        val locale = AppCompatDelegate.getApplicationLocales().get(0) ?: java.util.Locale.ENGLISH
        val timeString = DateFormat.getTimeInstance(DateFormat.SHORT, locale).format(Date(at))

        setContent {
            GlucoTimeTheme {
                CompositionLocalProvider(
                    LocalLayoutDirection provides if (locale.language == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        AlarmScreen(
                            label = localizedLabel(rawLabel),
                            time = timeString,
                            onAcknowledge = {
                                Log.d(TAG, "ACKNOWLEDGE_CLICK reminderId=$reminderId occurrence=$occurrence")
                                AlarmRingingService.stopActiveAlarm(this@AlarmActivity, profileId, reminderId, occurrence, AlarmOccurrenceState.ACKNOWLEDGED)
                                finishAndRemoveTask()
                            }
                        )
                    }
                }
            }
        }
    }
    
    @Composable
    private fun localizedLabel(label: String): String = when (label) {
        "Morning" -> stringResource(R.string.morning_label)
        "Noon" -> stringResource(R.string.noon_label)
        "Evening" -> stringResource(R.string.evening_label)
        else -> label
    }

    companion object {
        private const val TAG = "AlarmActivity"
        private var activeReference: WeakReference<AlarmActivity>? = null

        fun closeIfVisible() {
            activeReference?.get()?.let { activity -> activity.runOnUiThread { activity.finishAndRemoveTask() } }
        }
        const val REMINDER_ID = "alarm_reminder_id"
        const val PROFILE_ID = "alarm_profile_id"
        const val LABEL = "alarm_label"
        const val SCHEDULED_AT = "alarm_scheduled_at"
        const val OCCURRENCE_AT = "alarm_occurrence_at"
    }
}

@Composable
private fun AlarmScreen(label: String, time: String, onAcknowledge: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .scale(scale)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.Alarm,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Text(
            text = time,
            style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = label.ifEmpty { stringResource(R.string.alarm_notification_title) },
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = stringResource(R.string.time_for_reading),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(64.dp))
        
        Button(
            onClick = onAcknowledge,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = MaterialTheme.shapes.large,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Icon(Icons.Outlined.CheckCircle, contentDescription = null)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                stringResource(R.string.alarm_acknowledge),
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
}
