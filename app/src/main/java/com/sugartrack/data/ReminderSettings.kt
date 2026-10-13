package com.sugartrack.data

enum class RepeatMode { EVERY_DAYS, EVERY_X_DAYS, SELECTED_WEEKDAYS }

data class Reminder(
    val id: Long,
    val enabled: Boolean = true,
    val label: String = "Reminder",
    val hour: Int = 8,
    val minute: Int = 0,
    val repeatMode: RepeatMode = RepeatMode.EVERY_DAYS,
    val selectedWeekdays: Set<Int> = emptySet(),
    val intervalDays: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt,
    val profileId: Long = 1
)
