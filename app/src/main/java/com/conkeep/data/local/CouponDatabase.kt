package com.conkeep.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.conkeep.data.local.dao.CouponDao
import com.conkeep.data.local.entity.CouponEntity
import com.conkeep.data.remote.dto.CouponTypeConverters

@TypeConverters(CouponTypeConverters::class)
@Database(
    entities = [CouponEntity::class],
    version = 12,
    exportSchema = true,
)
abstract class CouponDatabase : RoomDatabase() {
    abstract fun couponDao(): CouponDao
}
