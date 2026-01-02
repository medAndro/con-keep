// di/DatabaseModule.kt
package com.conkeep.di

import android.content.Context
import androidx.room.Room
import com.conkeep.data.local.CouponDatabase
import com.conkeep.data.local.dao.CouponDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideCouponDatabase(
        @ApplicationContext context: Context,
    ): CouponDatabase =
        Room
            .databaseBuilder(
                context,
                CouponDatabase::class.java,
                "coupon_database",
            ).fallbackToDestructiveMigration(true) // 개발 중에만 사용
            .build()

    @Provides
    @Singleton
    fun provideCouponDao(database: CouponDatabase): CouponDao = database.couponDao()
}
