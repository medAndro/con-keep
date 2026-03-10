package com.conkeep.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.conkeep.data.local.dao.CouponDao
import com.conkeep.data.local.entity.CouponEntity
import com.conkeep.ui.feature.coupon.model.CouponSortType

@TypeConverters(CouponTypeConverters::class)
@Database(
    entities = [CouponEntity::class],
    version = 12,
    exportSchema = true,
)
abstract class CouponDatabase : RoomDatabase() {
    abstract fun couponDao(): CouponDao
}

class CouponTypeConverters {
    @TypeConverter
    fun fromSortType(type: CouponSortType): Int = type.sortType

    @TypeConverter
    fun toSortType(value: Int): CouponSortType = CouponSortType.entries.find { it.sortType == value } ?: CouponSortType.RECENT_ADD
}
