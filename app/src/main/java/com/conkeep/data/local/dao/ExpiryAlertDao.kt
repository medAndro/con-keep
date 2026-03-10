package com.conkeep.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.conkeep.data.local.entity.ExpiryAlertEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpiryAlertDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(expiryAlert: ExpiryAlertEntity): Long

    @Delete
    suspend fun delete(expiryAlert: ExpiryAlertEntity): Int // 삭제된 행 수

    @Query("SELECT * FROM expiry_alerts where user_id = :userId ORDER BY daysBefore ASC, targetTime ASC")
    fun getAlerts(userId: String): Flow<List<ExpiryAlertEntity>>
}
