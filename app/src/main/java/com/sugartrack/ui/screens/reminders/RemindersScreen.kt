package com.sugartrack.ui.screens.reminders

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.sugartrack.R
import com.sugartrack.data.Reminder
import com.sugartrack.data.ReminderPreferences
import com.sugartrack.data.RepeatMode
import com.sugartrack.reminders.ReminderScheduler
import com.sugartrack.ui.components.GlucoCard
import com.sugartrack.ui.components.GlucoOutlinedButton
import com.sugartrack.ui.components.GlucoPrimaryButton
import com.sugartrack.ui.components.GlucoTextField
import com.sugartrack.ui.components.TimeInputDialog
import com.sugartrack.ui.tutorial.LocalTutorialController
import com.sugartrack.ui.tutorial.TutorialStep
import com.sugartrack.ui.tutorial.TutorialTarget
import com.sugartrack.ui.tutorial.tutorialTarget
import com.sugartrack.ui.utils.displayTime
import com.sugartrack.ui.utils.localizedReminderLabel
import com.sugartrack.ui.utils.weekdayText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

@Composable
fun RemindersScreen(profileId: Long) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var list by remember(profileId) { mutableStateOf(emptyList<Reminder>()) }
    var editor by remember { mutableStateOf<Reminder?>(null) }
    var editorIsNewReminder by remember { mutableStateOf(false) }
    var showExactAlarmDialog by remember { mutableStateOf(false) }
    
    val tutorial = LocalTutorialController.current
    
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, list) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val scheduler = ReminderScheduler(context)
                if (scheduler.exactAlarmAccessAvailable()) {
                    scope.launch(Dispatchers.IO) { scheduler.rescheduleRegular(list) }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(profileId) {
        list = ReminderPreferences(context).read(profileId)
    }

    fun save(next: List<Reminder>, advanceTutorialAfterSave: Boolean = false) {
        val removedIds = list.map { it.id }.toSet() - next.map { it.id }.toSet()
        list = next
        if (!ReminderScheduler(context).exactAlarmAccessAvailable()) showExactAlarmDialog = true

        scope.launch {
            val scheduler = ReminderScheduler(context)
            removedIds.forEach { scheduler.cancel(it, profileId) }
            ReminderPreferences(context).write(profileId, next)
            scheduler.rescheduleAll(next)
            if (advanceTutorialAfterSave) tutorial?.onReminderSaved()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = stringResource(R.string.manage_reminders), style = MaterialTheme.typography.headlineMedium)

        GlucoPrimaryButton(
            text = stringResource(R.string.set_reminder),
            icon = Icons.Outlined.Add,
            modifier = Modifier.fillMaxWidth().tutorialTarget(TutorialTarget.ADD_REMINDER_BUTTON),
            onClick = {
                val now = System.currentTimeMillis()
                tutorial?.clearTarget(TutorialTarget.ADD_REMINDER_BUTTON)
                editorIsNewReminder = true
                editor = Reminder(
                    id = (list.maxOfOrNull { it.id } ?: 0) + 1,
                    label = context.getString(R.string.new_reminder),
                    createdAt = now,
                    updatedAt = now,
                    profileId = profileId
                )
            }
        )

        list.forEach { reminder ->
            GlucoCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = localizedReminderLabel(reminder.label),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            displayTime(reminder.hour, reminder.minute),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(onClick = { editorIsNewReminder = false; editor = reminder }) {
                                Icon(Icons.Outlined.Edit, contentDescription = stringResource(R.string.edit))
                            }
                            IconButton(onClick = { save(list.filterNot { it.id == reminder.id }) }) {
                                Icon(Icons.Outlined.DeleteOutline, contentDescription = stringResource(R.string.delete), tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                    Switch(
                        checked = reminder.enabled,
                        onCheckedChange = {
                            save(
                                list.map {
                                    if (it.id == reminder.id) it.copy(enabled = !reminder.enabled, updatedAt = System.currentTimeMillis())
                                    else it
                                }
                            )
                        }
                    )
                }
            }
        }
    }

    editor?.let { old ->
        ReminderEditor(
            initial = old,
            save = { newReminder ->
                save(
                    list.filterNot { it.id == old.id } + newReminder.copy(updatedAt = System.currentTimeMillis()),
                    advanceTutorialAfterSave = editorIsNewReminder
                )
                editor = null
                editorIsNewReminder = false
            },
            close = { editor = null; editorIsNewReminder = false }
        )
    }

    if (showExactAlarmDialog) {
        AlertDialog(
            onDismissRequest = { showExactAlarmDialog = false },
            title = { Text(stringResource(R.string.exact_alarm_title)) },
            text = { Text(stringResource(R.string.exact_alarm_message)) },
            dismissButton = { TextButton(onClick = { showExactAlarmDialog = false }) { Text(stringResource(R.string.cancel)) } },
            confirmButton = {
                TextButton(onClick = {
                    showExactAlarmDialog = false
                    if (Build.VERSION.SDK_INT >= 31) {
                        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        if (intent.resolveActivity(context.packageManager) != null) context.startActivity(intent)
                    }
                }) { Text(stringResource(R.string.exact_alarm_action)) }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReminderEditor(initial: Reminder, save: (Reminder) -> Unit, close: () -> Unit) {
    var reminder by remember(initial.id) { mutableStateOf(initial) }
    var showTimeDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = close,
        title = {
            Text(
                stringResource(
                    if (initial.id == 0L || initial.label == context.getString(R.string.new_reminder)) R.string.add_reminder
                    else R.string.edit_reminder
                )
            )
        },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                GlucoTextField(
                    value = reminder.label,
                    onValueChange = { reminder = reminder.copy(label = it) },
                    label = stringResource(R.string.label)
                )
                
                GlucoOutlinedButton(
                    text = stringResource(R.string.time, displayTime(reminder.hour, reminder.minute)),
                    onClick = { showTimeDialog = true },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Text(stringResource(R.string.recurrence), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = reminder.repeatMode == RepeatMode.EVERY_DAYS,
                        onClick = { reminder = reminder.copy(repeatMode = RepeatMode.EVERY_DAYS, intervalDays = 1) },
                        label = { Text(stringResource(R.string.every_day)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                    FilterChip(
                        selected = reminder.repeatMode == RepeatMode.EVERY_X_DAYS,
                        onClick = { reminder = reminder.copy(repeatMode = RepeatMode.EVERY_X_DAYS, intervalDays = reminder.intervalDays.coerceAtLeast(2)) },
                        label = { Text(stringResource(R.string.every_days)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                    FilterChip(
                        selected = reminder.repeatMode == RepeatMode.SELECTED_WEEKDAYS,
                        onClick = { reminder = reminder.copy(repeatMode = RepeatMode.SELECTED_WEEKDAYS) },
                        label = { Text(stringResource(R.string.weekdays)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
                
                if (reminder.repeatMode == RepeatMode.EVERY_X_DAYS) {
                    GlucoTextField(
                        value = reminder.intervalDays.coerceAtLeast(2).toString(),
                        onValueChange = { it.toIntOrNull()?.takeIf { days -> days >= 2 }?.let { days -> reminder = reminder.copy(intervalDays = days) } },
                        label = stringResource(R.string.number_of_days)
                    )
                } else if (reminder.repeatMode == RepeatMode.SELECTED_WEEKDAYS) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        listOf(Calendar.SUNDAY, Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY).forEach { day ->
                            FilterChip(
                                selected = day in reminder.selectedWeekdays,
                                onClick = {
                                    reminder = reminder.copy(
                                        selectedWeekdays = if (day in reminder.selectedWeekdays) reminder.selectedWeekdays - day else reminder.selectedWeekdays + day
                                    )
                                },
                                label = { Text(weekdayText(day)) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = reminder.label.trim().isNotEmpty(),
                onClick = { save(reminder.copy(label = reminder.label.trim())) }
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = close) { Text(stringResource(R.string.cancel)) }
        }
    )

    if (showTimeDialog) {
        TimeInputDialog(
            hour24 = reminder.hour,
            minute = reminder.minute,
            onSave = { hour, minute -> reminder = reminder.copy(hour = hour, minute = minute); showTimeDialog = false },
            onClose = { showTimeDialog = false }
        )
    }
}
