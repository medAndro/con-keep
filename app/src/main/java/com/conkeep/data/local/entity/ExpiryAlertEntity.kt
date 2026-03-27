package com.conkeep.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import com.conkeep.ui.feature.setting.notification.CouponAlarmSetting

@Entity(
    tableName = "expiry_alerts",
    primaryKeys = ["user_id", "daysBefore", "targetTime"],
)
data class ExpiryAlertEntity(
    @ColumnInfo(name = "user_id")
    val userId: String,
    @Embedded
    val couponAlarmSetting: CouponAlarmSetting,
)
