package com.sugartrack.ui.screens.history

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sugartrack.R
import com.sugartrack.data.GlucoseReading
import com.sugartrack.data.GlucoseRepository
import com.sugartrack.data.SugarTrackDatabase
import com.sugartrack.data.export.ExcelExporter
import com.sugartrack.data.export.PdfExporter
import com.sugartrack.ui.components.GlucoCard
import com.sugartrack.ui.components.GlucoDialog
import com.sugartrack.ui.components.GlucoOutlinedButton
import com.sugartrack.ui.tutorial.LocalTutorialController
import com.sugartrack.ui.tutorial.TutorialStep
import com.sugartrack.ui.tutorial.TutorialTarget
import com.sugartrack.ui.tutorial.tutorialTarget
import com.sugartrack.ui.utils.measurementText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.*

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(profileId: Long, userName: String, onEdit: (GlucoseReading) -> Unit) {
    val context = LocalContext.current
    val repo = remember(profileId) { GlucoseRepository(SugarTrackDatabase.get(context), profileId) }
    val readings by repo.all.collectAsStateWithLifecycle(initialValue = emptyList())
    val locale = AppCompatDelegate.getApplicationLocales().get(0) ?: Locale.ENGLISH
    val scope = rememberCoroutineScope()
    val tutorial = LocalTutorialController.current
    
    var dateFilter by remember { mutableStateOf<java.time.LocalDate?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var periodMonths by remember { mutableStateOf<Int?>(null) }
    var deleteTarget by remember { mutableStateOf<GlucoseReading?>(null) }
    
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.IO) {
            val success = runCatching {
                context.contentResolver.openOutputStream(uri)?.use {
                    ExcelExporter.write(context, readings, it)
                } ?: error("Unable to open export destination")
            }.isSuccess
            withContext(Dispatchers.Main) {
                Toast.makeText(context, context.getString(if (success) R.string.history_exported else R.string.history_export_failed), Toast.LENGTH_LONG).show()
            }
        }
    }
    
    val pdfExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.IO) {
            val success = runCatching {
                context.contentResolver.openOutputStream(uri)?.use {
                    PdfExporter.write(context, readings, userName, it)
                } ?: error("Unable to open export destination")
            }.isSuccess
            withContext(Dispatchers.Main) {
                Toast.makeText(context, context.getString(if (success) R.string.history_pdf_exported else R.string.history_pdf_export_failed), Toast.LENGTH_LONG).show()
            }
        }
    }
    
    val now = java.time.ZonedDateTime.now()
    val periodStart = periodMonths?.let { now.minusMonths(it.toLong()) }
    val periodReadings = readings.filter { reading ->
        periodStart == null || java.time.Instant.ofEpochMilli(reading.measuredAt)
            .atZone(java.time.ZoneId.systemDefault())
            .toInstant() >= periodStart.toInstant()
    }
    val visibleReadings = (dateFilter?.let { selectedDate ->
        readings.filter { reading ->
            java.time.Instant.ofEpochMilli(reading.measuredAt)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate() == selectedDate
        }
    } ?: periodReadings).sortedByDescending { it.measuredAt }
    
    val today = now.toLocalDate()
    val groups = visibleReadings
        .groupBy { java.time.Instant.ofEpochMilli(it.measuredAt).atZone(java.time.ZoneId.systemDefault()).toLocalDate() }
        .toList()

    val tutorialReading = if (tutorial?.active == true && tutorial.currentStep in listOf(TutorialStep.HISTORY_EDIT, TutorialStep.HISTORY_DELETE)) {
        visibleReadings.firstOrNull { tutorial.isTutorialReading(it.id) }
    } else {
        null
    }
    val tutorialTargetIndex = if (tutorialReading != null) {
        var itemIndex = 5
        var targetIndex: Int? = null
        groups.forEach { (_, dayReadings) ->
            val rowIndex = dayReadings.indexOfFirst { it.id == tutorialReading.id }
            if (rowIndex >= 0 && targetIndex == null) targetIndex = itemIndex + 1 + rowIndex
            itemIndex += 1 + dayReadings.size
        }
        targetIndex
    } else {
        null
    }
    val historyListState = rememberLazyListState()
    LaunchedEffect(tutorial?.currentStep, tutorialTargetIndex) {
        tutorialTargetIndex?.let { historyListState.animateScrollToItem(it) }
    }

    LazyColumn(
        state = historyListState,
        modifier = Modifier.padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 20.dp)
    ) {
        item {
            Text(text = stringResource(R.string.history), style = MaterialTheme.typography.headlineMedium)
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        dateFilter?.let {
                            DateFormat.getDateInstance(DateFormat.SHORT, locale)
                                .format(Date(it.atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()))
                        } ?: stringResource(R.string.search_by_date)
                    )
                }
                if (dateFilter != null) {
                    TextButton(onClick = { dateFilter = null }) {
                        Text(stringResource(R.string.clear_date))
                    }
                }
            }
        }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1 to R.string.one_month, 2 to R.string.two_months, 3 to R.string.three_months).forEach { (months, label) ->
                    FilterChip(
                        selected = periodMonths == months,
                        onClick = { periodMonths = months; dateFilter = null },
                        label = { Text(stringResource(label)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
                FilterChip(
                    selected = periodMonths == null,
                    onClick = { periodMonths = null; dateFilter = null },
                    label = { Text(stringResource(R.string.all_period)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }
        item {
            Text(
                if (dateFilter != null) {
                    stringResource(
                        R.string.date_reading_count,
                        DateFormat.getDateInstance(DateFormat.SHORT, locale).format(
                            Date(dateFilter!!.atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli())
                        ),
                        visibleReadings.size
                    )
                } else {
                    stringResource(R.string.reading_count, periodReadings.size)
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlucoOutlinedButton(
                    enabled = readings.isNotEmpty(),
                    onClick = { exportLauncher.launch(context.getString(R.string.app_name) + "_Readings_" + java.time.LocalDate.now() + ".xlsx") },
                    modifier = Modifier.weight(1f),
                    text = stringResource(R.string.export_excel)
                )
                GlucoOutlinedButton(
                    enabled = readings.isNotEmpty(),
                    onClick = { pdfExportLauncher.launch(context.getString(R.string.app_name) + "_Readings_" + java.time.LocalDate.now() + ".pdf") },
                    modifier = Modifier.weight(1f),
                    text = stringResource(R.string.export_pdf)
                )
            }
        }
        if (groups.isEmpty()) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 64.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Outlined.History,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = stringResource(if (dateFilter != null) R.string.no_readings_on_date else R.string.no_history),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        } else {
            groups.forEach { (date, dayReadings) ->
                item {
                    Text(
                        text = if (date == today) stringResource(R.string.today)
                        else DateFormat.getDateInstance(DateFormat.FULL, locale)
                            .format(Date(date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli())),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                items(dayReadings.size) { index ->
                    val reading = dayReadings[index]
                    var menuExpanded by remember(reading.id) { mutableStateOf(false) }
                    
                    val shouldHighlightTutorialReading = tutorial?.active == true &&
                        tutorial.currentStep in listOf(TutorialStep.HISTORY_EDIT, TutorialStep.HISTORY_DELETE) &&
                        tutorial.isTutorialReading(reading.id)
                    val cardModifier = if (shouldHighlightTutorialReading) {
                        Modifier.fillMaxWidth().tutorialTarget(TutorialTarget.HISTORY_ROW)
                    } else {
                        Modifier.fillMaxWidth()
                    }
                    
                    GlucoCard(modifier = cardModifier) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${reading.glucoseValue} • ${measurementText(reading.measurementType)}",
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    DateFormat.getTimeInstance(DateFormat.SHORT, locale).format(Date(reading.measuredAt)),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                reading.writtenNote?.takeIf { it.isNotBlank() }?.let { note ->
                                    CompositionLocalProvider(
                                        LocalLayoutDirection provides if (locale.language == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr
                                    ) {
                                        Column(Modifier.padding(top = 8.dp)) {
                                            Text(stringResource(R.string.note_label), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                            Text(text = note, style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                }
                            }
                            Box {
                                IconButton(onClick = { menuExpanded = true }) {
                                    Icon(Icons.Outlined.MoreVert, contentDescription = null)
                                }
                                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.edit)) },
                                        onClick = {
                                            menuExpanded = false
                                            tutorial?.clearTarget(TutorialTarget.HISTORY_ROW)
                                            onEdit(reading)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) },
                                        onClick = {
                                            menuExpanded = false
                                            tutorial?.clearTarget(TutorialTarget.HISTORY_ROW)
                                            deleteTarget = reading
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = (dateFilter ?: today).atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    java.time.Instant.ofEpochMilli(utcTimeMillis).atZone(java.time.ZoneOffset.UTC).toLocalDate() <= today
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        dateFilter = java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneOffset.UTC).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text(stringResource(R.string.date_picker_confirm)) }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.cancel)) } }
        ) { DatePicker(state = datePickerState) }
    }

    deleteTarget?.let { target ->
        GlucoDialog(
            onDismissRequest = { deleteTarget = null },
            title = stringResource(R.string.confirm_delete_reading),
            confirmButtonText = stringResource(R.string.delete),
            onConfirm = {
                scope.launch {
                    repo.delete(target)
                    tutorial?.onReadingDeleted(target.id)
                    deleteTarget = null
                }
            },
            dismissButtonText = stringResource(R.string.cancel)
        ) {
            Text(stringResource(R.string.delete_reading_warning))
        }
    }
}
