package com.sugartrack.ui.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Bloodtype
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sugartrack.R
import com.sugartrack.data.GlucoseRepository
import com.sugartrack.data.Reminder
import com.sugartrack.data.ReminderPreferences
import com.sugartrack.data.SugarTrackDatabase
import com.sugartrack.reminders.ReminderScheduleCalculator
import com.sugartrack.ui.components.GlucoCard
import com.sugartrack.ui.components.GlucoPrimaryButton
import com.sugartrack.ui.screens.reading.AddEditReadingScreen
import com.sugartrack.ui.tutorial.TutorialTarget
import com.sugartrack.ui.tutorial.tutorialTarget
import com.sugartrack.ui.utils.formatLocalDate
import com.sugartrack.ui.utils.localizedReminderLabel
import com.sugartrack.ui.utils.measurementText
import java.text.DateFormat
import java.util.*

@Composable
fun HomeScreen(
    userName: String,
    profileId: Long,
    language: String,
    onSetReminder: () -> Unit,
    startAdding: Boolean = false,
    alarmOccurrence: Long? = null,
    alarmReminderId: Long? = null,
    alarmProfileId: Long? = null,
    onAddReadingAction: () -> Unit = {},
    onAlarmNavigationFinished: () -> Unit = {}
) {
    val context = LocalContext.current
    val repo = remember(profileId) {
        GlucoseRepository(SugarTrackDatabase.get(context), profileId)
    }

    var day by remember { mutableStateOf(java.time.LocalDate.now()) }
    var adding by remember(startAdding) { mutableStateOf(startAdding) }

    val readings by repo.readingsFor(day).collectAsStateWithLifecycle(initialValue = emptyList())
    val today by repo.countToday().collectAsStateWithLifecycle(initialValue = 0)
    val week by repo.countWeek().collectAsStateWithLifecycle(initialValue = 0)
    val month by repo.countMonth().collectAsStateWithLifecycle(initialValue = 0)

    val reminders by produceState(initialValue = emptyList<Reminder>(), profileId) {
        value = ReminderPreferences(context).read(profileId)
    }

    if (adding) {
        AddEditReadingScreen(
            repo = repo,
            selectedDate = day,
            alarmOccurrence = alarmOccurrence,
            alarmReminderId = alarmReminderId,
            alarmProfileId = alarmProfileId,
            done = {
                adding = false
                if (startAdding) onAlarmNavigationFinished()
            }
        )
        return
    }

    val nextReminder = reminders
        .filter { it.enabled }
        .map { it to ReminderScheduleCalculator.nextTrigger(it) }
        .filter { it.second != Long.MAX_VALUE }
        .minByOrNull { it.second }

    val locale = if (language == "ar") Locale.forLanguageTag("ar") else Locale.ENGLISH
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 20.dp, bottom = 24.dp)
    ) {
        item {
            Text(text = stringResource(R.string.hello, userName), style = MaterialTheme.typography.headlineMedium)
            Text(DateFormat.getDateInstance(DateFormat.FULL, locale).format(Date()), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        item {
            GlucoPrimaryButton(
                text = stringResource(if (day == java.time.LocalDate.now()) R.string.add_todays_reading else R.string.add_reading_for_date, day.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy", locale))),
                onClick = { 
                    adding = true 
                    onAddReadingAction() 
                },
                icon = Icons.Outlined.Add,
                modifier = Modifier.fillMaxWidth().tutorialTarget(TutorialTarget.ADD_READING_BUTTON)
            )
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Stat(modifier = Modifier.weight(1f), label = R.string.today, value = today)
                Stat(modifier = Modifier.weight(1f), label = R.string.this_week, value = week)
                Stat(modifier = Modifier.weight(1f), label = R.string.this_month, value = month)
            }
        }

        item {
            GlucoCard(modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(R.string.next_reminder), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                if (nextReminder != null) {
                    Text(
                        text = DateFormat.getTimeInstance(DateFormat.SHORT, locale).format(Date(nextReminder.second)),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(localizedReminderLabel(nextReminder.first.label), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                } else {
                    Text(stringResource(R.string.no_reminder), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                    TextButton(onClick = onSetReminder) { Text(stringResource(R.string.set_reminder), color = MaterialTheme.colorScheme.primary) }
                }
            }
        }

        item {
            Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(percent = 50), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    IconButton(onClick = { day = day.minusDays(1) }) { Icon(Icons.Outlined.ChevronLeft, contentDescription = stringResource(R.string.previous), modifier = Modifier.scale(scaleX = if (isRtl) -1f else 1f, scaleY = 1f)) }
                    Text(formatLocalDate(day, locale), style = MaterialTheme.typography.titleMedium)
                    IconButton(enabled = day.isBefore(java.time.LocalDate.now()), onClick = { if (day.isBefore(java.time.LocalDate.now())) day = day.plusDays(1) }) { Icon(Icons.Outlined.ChevronRight, contentDescription = stringResource(R.string.next), modifier = Modifier.scale(scaleX = if (isRtl) -1f else 1f, scaleY = 1f)) }
                }
            }
        }

        item {
            Text(
                text = if (day == java.time.LocalDate.now()) stringResource(R.string.todays_readings) else stringResource(R.string.readings_for_date, day.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy", locale))),
                style = MaterialTheme.typography.titleMedium
            )
        }

        items(readings) { reading ->
            GlucoCard(modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(reading.glucoseValue.toString(), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(6.dp))
                            Text("mg/dL", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 4.dp))
                        }
                        Text(measurementText(reading.measurementType), style = MaterialTheme.typography.titleMedium)
                        Text(DateFormat.getTimeInstance(DateFormat.SHORT, locale).format(Date(reading.measuredAt)), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (!reading.writtenNote.isNullOrBlank()) {
                            Text(stringResource(R.string.note_attached), color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Icon(Icons.Outlined.Bloodtype, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(28.dp))
                }
            }
        }
    }
}

@Composable
private fun Stat(modifier: Modifier = Modifier, label: Int, value: Int) {
    GlucoCard(modifier = modifier, paddingValues = PaddingValues(12.dp)) {
        Text(text = stringResource(label), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Text(text = value.toString(), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
    }
}
