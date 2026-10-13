package com.sugartrack.reminders

import com.sugartrack.data.*
import org.junit.Test
import kotlin.test.assertEquals
import java.util.Calendar

class ReminderScheduleCalculatorTest {
    @Test fun intervalSkipsToNextConfiguredDay() { val from=Calendar.getInstance().apply { set(2026,Calendar.AUGUST,29,10,0,0);set(Calendar.MILLISECOND,0) }.timeInMillis; val r=Reminder(1,hour=9,minute=0,intervalDays=2); val c=Calendar.getInstance().apply { timeInMillis=ReminderScheduleCalculator.nextTrigger(r,from) }; assertEquals(31,c.get(Calendar.DAY_OF_MONTH)); assertEquals(9,c.get(Calendar.HOUR_OF_DAY)) }
    @Test fun weekdaysSelectsNextMatchingDay() { val from=Calendar.getInstance().apply { set(2026,Calendar.AUGUST,29,10,0,0);set(Calendar.MILLISECOND,0) }.timeInMillis; val r=Reminder(1,hour=9,minute=0,repeatMode=RepeatMode.SELECTED_WEEKDAYS,selectedWeekdays=setOf(Calendar.MONDAY,Calendar.WEDNESDAY,Calendar.FRIDAY)); val c=Calendar.getInstance().apply { timeInMillis=ReminderScheduleCalculator.nextTrigger(r,from) }; assertEquals(Calendar.MONDAY,c.get(Calendar.DAY_OF_WEEK)) }
}
