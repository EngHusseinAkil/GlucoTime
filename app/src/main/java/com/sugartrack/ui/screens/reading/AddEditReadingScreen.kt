package com.sugartrack.ui.screens.reading

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sugartrack.R
import com.sugartrack.data.GlucoseInput
import com.sugartrack.data.GlucoseReading
import com.sugartrack.data.GlucoseRepository
import com.sugartrack.data.MeasurementType
import com.sugartrack.reminders.AlarmOccurrenceState
import com.sugartrack.reminders.AlarmRingingService
import com.sugartrack.ui.components.GlucoOutlinedButton
import com.sugartrack.ui.components.GlucoPrimaryButton
import com.sugartrack.ui.components.GlucoTextField
import com.sugartrack.ui.components.TimeInputDialog
import com.sugartrack.ui.tutorial.LocalTutorialController
import com.sugartrack.ui.tutorial.TutorialStep
import com.sugartrack.ui.tutorial.TutorialTarget
import com.sugartrack.ui.tutorial.tutorialTarget
import com.sugartrack.ui.utils.formatLocalDate
import com.sugartrack.ui.utils.measurementText
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.*

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AddEditReadingScreen(
    repo: GlucoseRepository,
    initial: GlucoseReading? = null,
    selectedDate: java.time.LocalDate? = null,
    alarmOccurrence: Long? = null,
    alarmReminderId: Long? = null,
    alarmProfileId: Long? = null,
    done: () -> Unit
) {
    val context = LocalContext.current
    val locale = AppCompatDelegate.getApplicationLocales().get(0) ?: Locale.ENGLISH
    val zone = java.time.ZoneId.systemDefault()
    val initialDateTime = initial?.let { java.time.Instant.ofEpochMilli(it.measuredAt).atZone(zone) }
    
    var value by remember(initial?.id) { mutableStateOf(initial?.glucoseValue?.let { GlucoseInput.formatValue(it) } ?: "") }
    var note by remember(initial?.id) { mutableStateOf(initial?.writtenNote.orEmpty()) }
    var type by remember(initial?.id) { mutableStateOf(initial?.measurementType ?: MeasurementType.RANDOM) }
    var selectedDateValue by remember(initial?.id, selectedDate) {
        mutableStateOf(initialDateTime?.toLocalDate() ?: selectedDate ?: java.time.LocalDate.now())
    }
    var selectedTime by remember(initial?.id) { mutableStateOf(initialDateTime?.toLocalTime() ?: java.time.LocalTime.now().withSecond(0).withNano(0)) }
    
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<Int?>(null) }
    var inputError by remember(initial?.id) { mutableStateOf<Int?>(null) }

    val scope = rememberCoroutineScope()
    val tutorial = LocalTutorialController.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = if (initial == null) stringResource(R.string.add_reading_for_date, formatLocalDate(selectedDateValue, locale)) else stringResource(R.string.edit_reading),
            style = MaterialTheme.typography.headlineMedium
        )

        GlucoTextField(
            value = value,
            onValueChange = {
                if (GlucoseInput.isWithinMaximum(it)) {
                    value = it
                    inputError = null
                } else {
                    inputError = R.string.glucose_max_typing
                }
            },
            label = stringResource(R.string.glucose_value),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )

        inputError?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }

        Text(stringResource(R.string.measurement_type), style = MaterialTheme.typography.titleMedium)

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MeasurementType.values().forEach { measurement ->
                FilterChip(
                    selected = type == measurement,
                    onClick = { type = measurement },
                    label = { Text(measurementText(measurement)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }

        GlucoOutlinedButton(
            text = stringResource(R.string.reading_date) + ": " + DateFormat.getDateInstance(DateFormat.SHORT, locale).format(Date(selectedDateValue.atStartOfDay(zone).toInstant().toEpochMilli())),
            onClick = { showDatePicker = true },
            icon = Icons.Outlined.CalendarToday,
            modifier = Modifier.fillMaxWidth()
        )

        GlucoOutlinedButton(
            text = stringResource(R.string.reading_time) + ": " + DateFormat.getTimeInstance(DateFormat.SHORT, locale).format(Date(selectedDateValue.atTime(selectedTime).atZone(zone).toInstant().toEpochMilli())),
            onClick = { showTimePicker = true },
            icon = Icons.Outlined.Schedule,
            modifier = Modifier.fillMaxWidth()
        )

        GlucoTextField(
            value = note,
            onValueChange = { note = it },
            label = stringResource(R.string.written_note),
            singleLine = false
        )

        val isGlucoseValid = GlucoseInput.normalizeDigits(value).toDoubleOrNull()?.let {
            it in GlucoseInput.MIN_GLUCOSE_MG_DL..GlucoseInput.MAX_GLUCOSE_MG_DL
        } == true

        GlucoPrimaryButton(
            text = stringResource(if (initial == null) R.string.save_reading else R.string.save_changes),
            enabled = isGlucoseValid,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                val normalizedValue = GlucoseInput.normalizeDigits(value)
                val glucoseValue = normalizedValue.toDoubleOrNull()
                val measuredAt = selectedDateValue.atTime(selectedTime).atZone(zone).toInstant().toEpochMilli()
                
                validationError = when {
                    glucoseValue != null && glucoseValue > GlucoseInput.MAX_GLUCOSE_MG_DL -> R.string.glucose_max_save
                    glucoseValue == null || glucoseValue !in GlucoseInput.MIN_GLUCOSE_MG_DL..GlucoseInput.MAX_GLUCOSE_MG_DL -> R.string.invalid_glucose_value
                    selectedDateValue.isAfter(java.time.LocalDate.now()) || measuredAt > System.currentTimeMillis() -> R.string.future_reading_datetime
                    else -> null
                }
                
                if (validationError == null) {
                    scope.launch {
                        val reading = initial?.copy(
                            glucoseValue = glucoseValue!!, measuredAt = measuredAt,
                            measurementType = type, writtenNote = note.takeIf { it.isNotBlank() },
                            updatedAt = System.currentTimeMillis()
                        ) ?: GlucoseReading(
                            glucoseValue = glucoseValue!!, measuredAt = measuredAt,
                            measurementType = type, writtenNote = note.takeIf { it.isNotBlank() },
                            profileId = repo.profileId
                        )
                        if (initial == null) {
                            val readingId = repo.insert(reading)
                            when (tutorial?.currentStep) {
                                TutorialStep.ADD_PAST_READING -> tutorial.onPastReadingSaved(readingId, reading.measuredAt)
                                else -> tutorial?.onReadingSaved(readingId, reading.measuredAt)
                            }
                        } else {
                            val changed = initial.glucoseValue != reading.glucoseValue ||
                                initial.measuredAt != reading.measuredAt ||
                                initial.measurementType != reading.measurementType ||
                                initial.writtenNote != reading.writtenNote
                            repo.update(reading)
                            tutorial?.onReadingUpdated(reading.id, changed)
                        }
                        
                        val occurrence = alarmOccurrence
                        val occurrenceProfile = alarmProfileId
                        val occurrenceReminder = alarmReminderId
                        if (occurrence != null && occurrenceProfile != null && occurrenceReminder != null && reading.profileId == occurrenceProfile && reading.measuredAt >= occurrence) {
                            AlarmRingingService.stopActiveAlarm(context, occurrenceProfile, occurrenceReminder, occurrence, AlarmOccurrenceState.COMPLETED)
                        }

                        done()
                    }
                }
            }
        )

        validationError?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }

        TextButton(onClick = done) {
            Text(stringResource(R.string.cancel))
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDateValue.atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    java.time.Instant.ofEpochMilli(utcTimeMillis).atZone(java.time.ZoneOffset.UTC).toLocalDate() <= java.time.LocalDate.now()
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        selectedDateValue = java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneOffset.UTC).toLocalDate()
                    }
                    validationError = null
                    showDatePicker = false
                }) { Text(stringResource(R.string.date_picker_confirm)) }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.cancel)) } }
        ) { DatePicker(state = datePickerState) }
    }

    if (showTimePicker) {
        TimeInputDialog(
            hour24 = selectedTime.hour,
            minute = selectedTime.minute,
            onSave = { hour, minute ->
                selectedTime = java.time.LocalTime.of(hour, minute)
                validationError = null
                showTimePicker = false
            },
            onClose = { showTimePicker = false }
        )
    }
}
