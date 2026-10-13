package com.sugartrack.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.sugartrack.R
import com.sugartrack.data.MeasurementType
import java.text.DateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

fun formatLocalDate(date: java.time.LocalDate, locale: Locale): String {
    val millis = date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
    return DateFormat.getDateInstance(DateFormat.FULL, locale).format(Date(millis))
}

@Composable
fun localizedReminderLabel(label: String): String = when (label) {
    "Morning" -> stringResource(R.string.morning_label)
    "Noon" -> stringResource(R.string.noon_label)
    "Evening" -> stringResource(R.string.evening_label)
    else -> label
}

@Composable
fun measurementText(type: MeasurementType): String {
    return stringResource(
        when (type) {
            MeasurementType.FASTING -> R.string.fasting
            MeasurementType.BEFORE_MEAL -> R.string.before_meal
            MeasurementType.AFTER_MEAL -> R.string.after_meal
            MeasurementType.RANDOM -> R.string.random
            MeasurementType.OTHER -> R.string.other
        }
    )
}

fun displayTime(hour: Int, minute: Int): String {
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
    }
    val locale = androidx.core.os.LocaleListCompat.getAdjustedDefault().get(0) ?: Locale.ENGLISH
    return DateFormat.getTimeInstance(DateFormat.SHORT, locale).format(calendar.time)
}

@Composable
fun weekdayText(day: Int): String {
    return stringResource(
        when (day) {
            Calendar.MONDAY -> R.string.monday
            Calendar.TUESDAY -> R.string.tuesday
            Calendar.WEDNESDAY -> R.string.wednesday
            Calendar.THURSDAY -> R.string.thursday
            Calendar.FRIDAY -> R.string.friday
            Calendar.SATURDAY -> R.string.saturday
            else -> R.string.sunday
        }
    )
}
