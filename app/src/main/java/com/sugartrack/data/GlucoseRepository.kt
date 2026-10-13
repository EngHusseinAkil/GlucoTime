package com.sugartrack.data

import kotlinx.coroutines.flow.Flow
import java.time.*

class GlucoseRepository(private val db:SugarTrackDatabase, val profileId:Long=1) {
    val all:Flow<List<GlucoseReading>> = db.readings().observeAll(profileId)
    fun readingsFor(day:LocalDate)=db.readings().observeBetween(profileId,day.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),day.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli())
    fun latest()=db.readings().observeLatest(profileId)
    fun countToday()=countBetween(LocalDate.now(),LocalDate.now().plusDays(1))
    fun countWeek():Flow<Int>{val d=LocalDate.now(); val start=d.minusDays((d.dayOfWeek.value-1).toLong()); return countBetween(start,start.plusDays(7))}
    fun countMonth():Flow<Int>{val d=LocalDate.now(); val start=d.withDayOfMonth(1); return countBetween(start,start.plusMonths(1))}
    private fun countBetween(a:LocalDate,b:LocalDate)=db.readings().countBetween(profileId,a.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),b.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli())
    suspend fun insert(r:GlucoseReading)=db.readings().insert(r); suspend fun update(r:GlucoseReading)=db.readings().update(r); suspend fun delete(r:GlucoseReading)=db.readings().delete(r)
    suspend fun hasReadingSince(since:Long)=db.readings().hasReadingSince(profileId,since)
    fun note(day:LocalDate)=db.notes().observe(profileId,day.toString()); suspend fun saveNote(n:DailyNote)=db.notes().upsert(n.copy(profileId=profileId))
}
