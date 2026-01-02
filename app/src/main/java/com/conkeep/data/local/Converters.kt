package com.conkeep.data.local

import androidx.room.TypeConverter
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class Converters {
    // ISO 8601 날짜 형식 변환
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    @TypeConverter
    fun fromTimestamp(value: Long?): Instant? = value?.let { Instant.ofEpochMilli(it) }

    @TypeConverter
    fun dateToTimestamp(date: Instant?): Long? = date?.toEpochMilli()

    // LocalDate 변환 (expiry_date용)
    @TypeConverter
    fun fromLocalDate(date: LocalDate?): String? = date?.format(dateFormatter)

    @TypeConverter
    fun toLocalDate(dateString: String?): LocalDate? = dateString?.let { LocalDate.parse(it, dateFormatter) }
}
