package com.conkeep.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.conkeep.data.local.dao.CouponDao
import com.conkeep.data.local.entity.CouponEntity

@Database(
    entities = [CouponEntity::class],
    version = 2,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class CouponDatabase : RoomDatabase() {
    abstract fun couponDao(): CouponDao
}
