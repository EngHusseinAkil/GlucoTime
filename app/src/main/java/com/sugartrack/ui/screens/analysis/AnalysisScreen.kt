package com.sugartrack.ui.screens.analysis

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sugartrack.R
import com.sugartrack.data.GlucoseReading
import com.sugartrack.data.GlucoseRepository
import com.sugartrack.data.MeasurementType
import com.sugartrack.data.SugarTrackDatabase
import com.sugartrack.ui.utils.measurementText
import com.sugartrack.ui.components.GlucoCard
import java.text.DateFormat
import java.util.*

@Composable
fun AnalysisScreen(profileId: Long, onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember(profileId) { GlucoseRepository(SugarTrackDatabase.get(context), profileId) }
    val allReadings by repo.all.collectAsStateWithLifecycle(initialValue = emptyList())
    var period by remember { mutableStateOf(0) }
    
    val now = System.currentTimeMillis()
    val start = when (period) {
        0 -> now - 7L * 24 * 60 * 60 * 1000
        1 -> java.time.Instant.ofEpochMilli(now).atZone(java.time.ZoneId.systemDefault()).minusMonths(1).toInstant().toEpochMilli()
        2 -> java.time.Instant.ofEpochMilli(now).atZone(java.time.ZoneId.systemDefault()).minusMonths(3).toInstant().toEpochMilli()
        else -> null
    }
    
    val readings = allReadings.filter { start == null || it.measuredAt >= start }.sortedBy { it.measuredAt }
    val locale = AppCompatDelegate.getApplicationLocales().get(0) ?: Locale.ENGLISH
    val zone = java.time.ZoneId.systemDefault()
    val recordedDays = readings.map { java.time.Instant.ofEpochMilli(it.measuredAt).atZone(zone).toLocalDate() }.distinct().size
    val averagePerDay = if (recordedDays == 0) 0.0 else readings.size.toDouble() / recordedDays
    val minimum = readings.minOfOrNull { it.glucoseValue }
    val maximum = readings.maxOfOrNull { it.glucoseValue }
    val average = readings.takeIf { it.isNotEmpty() }?.map { it.glucoseValue }?.average()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TextButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.back))
        }
        Text(stringResource(R.string.data_analysis), style = MaterialTheme.typography.headlineMedium)
        
        @OptIn(ExperimentalLayoutApi::class)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(R.string.analysis_7_days, R.string.analysis_1_month, R.string.analysis_3_months, R.string.analysis_all).forEachIndexed { index, label ->
                FilterChip(
                    selected = period == index,
                    onClick = { period = index },
                    label = { Text(stringResource(label)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }

        Text(stringResource(R.string.analysis_disclaimer), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        
        if (readings.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 64.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Outlined.Analytics,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = stringResource(R.string.analysis_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AnalysisStatCard(stringResource(R.string.analysis_total), readings.size.toString(), Modifier.weight(1f))
                AnalysisStatCard(stringResource(R.string.analysis_average), "%.1f mg/dL".format(locale, average), Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AnalysisStatCard(stringResource(R.string.analysis_min), "%.1f mg/dL".format(locale, minimum), Modifier.weight(1f))
                AnalysisStatCard(stringResource(R.string.analysis_max), "%.1f mg/dL".format(locale, maximum), Modifier.weight(1f))
            }

            Text(stringResource(R.string.analysis_trend), style = MaterialTheme.typography.titleLarge)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("%.0f mg/dL".format(locale, maximum), style = MaterialTheme.typography.bodySmall)
                Text("%.0f mg/dL".format(locale, minimum), style = MaterialTheme.typography.bodySmall)
            }
            ReadingTrendChart(readings, Modifier.fillMaxWidth().height(230.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, locale).format(Date(readings.first().measuredAt)), style = MaterialTheme.typography.bodySmall)
                Text(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, locale).format(Date(readings.last().measuredAt)), style = MaterialTheme.typography.bodySmall)
            }

            Text(stringResource(R.string.analysis_types), style = MaterialTheme.typography.titleLarge)
            MeasurementType.values().forEach { typeValue ->
                val count = readings.count { it.measurementType == typeValue }
                if (count > 0) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(measurementText(typeValue), Modifier.weight(1f))
                        Text("$count (${(count * 100 / readings.size)}%)")
                    }
                    LinearProgressIndicator(
                        progress = { count.toFloat() / readings.size },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }

            Text(stringResource(R.string.analysis_activity), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.analysis_days) + ": $recordedDays")
            Text(stringResource(R.string.analysis_daily_average) + ": " + "%.1f".format(locale, averagePerDay))
        }
    }
}

@Composable
private fun AnalysisStatCard(label: String, value: String, modifier: Modifier = Modifier) {
    GlucoCard(modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun ReadingTrendChart(readings: List<GlucoseReading>, modifier: Modifier = Modifier) {
    val min = readings.minOf { it.glucoseValue }
    val max = readings.maxOf { it.glucoseValue }
    val range = (max - min).coerceAtLeast(1.0)
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    
    Canvas(modifier) {
        repeat(4) { row ->
            val y = size.height * row / 3f
            drawLine(gridColor, Offset(0f, y), Offset(size.width, y), 1f)
        }
        val points = readings.mapIndexed { index, reading ->
            val x = if (readings.size == 1) size.width / 2f else index * size.width / (readings.size - 1).toFloat()
            val y = size.height - ((reading.glucoseValue - min) / range).toFloat() * size.height
            Offset(x, y)
        }
        if (points.size > 1) {
            val path = Path().apply {
                moveTo(points.first().x, points.first().y)
                points.drop(1).forEach { lineTo(it.x, it.y) }
            }
            drawPath(path, lineColor, style = Stroke(width = 4f, cap = StrokeCap.Round))
        }
        points.forEach { drawCircle(lineColor, radius = 5f, center = it) }
    }
}
