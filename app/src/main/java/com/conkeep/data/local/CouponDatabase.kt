package com.conkeep.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.conkeep.data.local.dao.CouponDao
import com.conkeep.data.local.entity.CouponEntity

@Database(
    entities = [CouponEntity::class],
    version = 4,
    exportSchema = true,
)
abstract class CouponDatabase : RoomDatabase() {
    abstract fun couponDao(): CouponDao
}
