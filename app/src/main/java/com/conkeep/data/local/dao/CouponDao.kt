package com.conkeep.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.conkeep.data.local.entity.CouponEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CouponDao {
    @Query("SELECT * FROM coupons WHERE user_id = :userId ORDER BY created_at DESC")
    fun getCouponsFlow(userId: String): Flow<List<CouponEntity>>

    @Query("SELECT * FROM coupons WHERE id = :id")
    fun getCouponFlow(id: String): Flow<CouponEntity?>

    @Query("SELECT * FROM coupons WHERE user_id = :userId AND is_used = 0 ORDER BY expiry_date ASC")
    fun getActiveCoupons(userId: String): Flow<List<CouponEntity>>

    @Query("SELECT * FROM coupons WHERE id = :id")
    suspend fun getCouponById(id: String): CouponEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(coupon: CouponEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(coupons: List<CouponEntity>)

    @Update
    suspend fun update(coupon: CouponEntity)

    @Query("UPDATE coupons SET is_used = 1, used_at = :usedAt WHERE id = :id")
    suspend fun markAsUsed(
        id: String,
        usedAt: Long,
    )

    @Delete
    suspend fun delete(coupon: CouponEntity)

    @Query("DELETE FROM coupons WHERE user_id = :userId")
    suspend fun deleteAllByUser(userId: String)
}
