package com.conkeep.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.conkeep.data.local.dao.ExpiryAlertDao
import com.conkeep.data.local.entity.ExpiryAlertEntity
import kotlinx.datetime.LocalTime

@TypeConverters(ExpiryAlertTypeConverters::class)
@Database(
    entities = [ExpiryAlertEntity::class],
    version = 5,
    exportSchema = true,
)
abstract class ExpiryAlertDatabase : RoomDatabase() {
    abstract fun expiryAlertDao(): ExpiryAlertDao
}

class ExpiryAlertTypeConverters {
    @TypeConverter
    fun fromLocalTime(time: LocalTime?): Long? = time?.toNanosecondOfDay()

    @TypeConverter
    fun toLocalTime(nano: Long?): LocalTime? = nano?.let { LocalTime.fromNanosecondOfDay(it) }
}
