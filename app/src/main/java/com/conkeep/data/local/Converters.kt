package com.conkeep.data.local

import androidx.room.TypeConverter
import com.conkeep.data.local.entity.CouponLocalStatus
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Suppress("unused")
class Converters {
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    @TypeConverter fun fromStatus(status: CouponLocalStatus?): String? = status?.name

    @TypeConverter fun toStatus(status: String?): CouponLocalStatus? =
        status?.let {
            runCatching {
                CouponLocalStatus.valueOf(
                    it,
                )
            }.getOrNull()
        }

    // timestamptz ↔ Instant
    @TypeConverter fun fromInstant(instant: Instant?): Long? = instant?.toEpochMilli()

    @TypeConverter fun toInstant(timestamp: Long?): Instant? =
        timestamp?.let {
            Instant.ofEpochMilli(it)
        }

    // date ↔ LocalDate
    @TypeConverter fun fromLocalDate(date: LocalDate?): String? = date?.format(dateFormatter)

    @TypeConverter fun toLocalDate(dateString: String?): LocalDate? = dateString?.let { LocalDate.parse(it, dateFormatter) }
}
