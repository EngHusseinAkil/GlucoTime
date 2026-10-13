package com.sugartrack.data

import androidx.room.*
import androidx.room.migration.Migration
import kotlinx.coroutines.flow.Flow

enum class MeasurementType { FASTING, BEFORE_MEAL, AFTER_MEAL, RANDOM, OTHER }
class MeasurementTypeConverter { @TypeConverter fun from(value:MeasurementType)=value.name; @TypeConverter fun to(value:String)=runCatching{MeasurementType.valueOf(value)}.getOrDefault(MeasurementType.OTHER) }

@Entity(tableName="user_profiles", indices=[Index(value=["name"],unique=true)])
data class UserProfile(@PrimaryKey(autoGenerate=true) val id:Long=0, val name:String, val createdAt:Long=System.currentTimeMillis(), val updatedAt:Long=createdAt)

@Entity(tableName="glucose_readings", indices=[Index("measuredAt"),Index("profileId")])
data class GlucoseReading(@PrimaryKey(autoGenerate=true) val id:Long=0, val glucoseValue:Double, val measuredAt:Long, val measurementType:MeasurementType, val writtenNote:String?=null, val voiceNotePath:String?=null, val createdAt:Long=System.currentTimeMillis(), val updatedAt:Long=createdAt, val profileId:Long=1)

@Entity(tableName="daily_notes", indices=[Index(value=["profileId","date"],unique=true),Index("profileId")])
data class DailyNote(@PrimaryKey(autoGenerate=true) val id:Long=0, val date:String, val writtenNote:String?=null, val voiceNotePath:String?=null, val createdAt:Long=System.currentTimeMillis(), val updatedAt:Long=createdAt, val profileId:Long=1)

@Dao interface UserProfileDao {
    @Query("SELECT * FROM user_profiles ORDER BY createdAt ASC") fun observeAll():Flow<List<UserProfile>>
    @Query("SELECT * FROM user_profiles WHERE id=:id LIMIT 1") suspend fun get(id:Long):UserProfile?
    @Insert suspend fun insert(profile:UserProfile):Long
    @Update suspend fun update(profile:UserProfile)
    @Delete suspend fun delete(profile:UserProfile)
}

@Dao interface GlucoseReadingDao {
    @Insert suspend fun insert(reading:GlucoseReading):Long
    @Update suspend fun update(reading:GlucoseReading)
    @Delete suspend fun delete(reading:GlucoseReading)
    @Query("DELETE FROM glucose_readings WHERE profileId=:profileId") suspend fun deleteForProfile(profileId:Long)
    @Query("SELECT * FROM glucose_readings WHERE id=:id AND profileId=:profileId") suspend fun getById(id:Long,profileId:Long):GlucoseReading?
    @Query("SELECT * FROM glucose_readings WHERE profileId=:profileId ORDER BY measuredAt DESC") fun observeAll(profileId:Long):Flow<List<GlucoseReading>>
    @Query("SELECT * FROM glucose_readings WHERE profileId=:profileId AND measuredAt>=:start AND measuredAt<:end ORDER BY measuredAt DESC") fun observeBetween(profileId:Long,start:Long,end:Long):Flow<List<GlucoseReading>>
    @Query("SELECT * FROM glucose_readings WHERE profileId=:profileId ORDER BY measuredAt DESC LIMIT 1") fun observeLatest(profileId:Long):Flow<GlucoseReading?>
    @Query("SELECT COUNT(*) FROM glucose_readings WHERE profileId=:profileId AND measuredAt>=:start AND measuredAt<:end") fun countBetween(profileId:Long,start:Long,end:Long):Flow<Int>
    @Query("SELECT EXISTS(SELECT 1 FROM glucose_readings WHERE profileId=:profileId AND measuredAt>=:since)") suspend fun hasReadingSince(profileId:Long,since:Long):Boolean
}

@Dao interface DailyNoteDao { @Query("SELECT * FROM daily_notes WHERE profileId=:profileId AND date=:date LIMIT 1") fun observe(profileId:Long,date:String):Flow<DailyNote?>; @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun upsert(note:DailyNote); @Query("DELETE FROM daily_notes WHERE id=:id AND profileId=:profileId") suspend fun delete(id:Long,profileId:Long); @Query("DELETE FROM daily_notes WHERE profileId=:profileId") suspend fun deleteForProfile(profileId:Long) }

@Database(entities=[UserProfile::class,GlucoseReading::class,DailyNote::class],version=2,exportSchema=false)
@TypeConverters(MeasurementTypeConverter::class)
abstract class SugarTrackDatabase:RoomDatabase(){ abstract fun profiles():UserProfileDao; abstract fun readings():GlucoseReadingDao; abstract fun notes():DailyNoteDao; companion object { @Volatile private var instance:SugarTrackDatabase?=null; private val MIGRATION_1_2=object:Migration(1,2){override fun migrate(db:androidx.sqlite.db.SupportSQLiteDatabase){db.execSQL("CREATE TABLE IF NOT EXISTS user_profiles (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL)");db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_user_profiles_name ON user_profiles(name)");db.execSQL("INSERT OR IGNORE INTO user_profiles(id,name,createdAt,updatedAt) VALUES(1,'User',strftime('%s','now')*1000,strftime('%s','now')*1000)");db.execSQL("ALTER TABLE glucose_readings ADD COLUMN profileId INTEGER NOT NULL DEFAULT 1");db.execSQL("CREATE INDEX IF NOT EXISTS index_glucose_readings_profileId ON glucose_readings(profileId)");db.execSQL("ALTER TABLE daily_notes ADD COLUMN profileId INTEGER NOT NULL DEFAULT 1");db.execSQL("DROP INDEX IF EXISTS index_daily_notes_date");db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_daily_notes_profileId_date ON daily_notes(profileId,date)");db.execSQL("CREATE INDEX IF NOT EXISTS index_daily_notes_profileId ON daily_notes(profileId)")}}; fun get(context:android.content.Context)=instance?: synchronized(this){instance?:Room.databaseBuilder(context.applicationContext,SugarTrackDatabase::class.java,"sugartrack.db").addMigrations(MIGRATION_1_2).build().also{instance=it}} } }
