package com.conkeep.data.mapper

import com.conkeep.data.local.entity.ExpiryAlertEntity
import com.conkeep.ui.feature.setting.CouponAlarmSetting

fun ExpiryAlertEntity.toCouponAlarmSetting(): CouponAlarmSetting = this.couponAlarmSetting

fun CouponAlarmSetting.toExpiryAlertEntity(userId: String): ExpiryAlertEntity =
    ExpiryAlertEntity(
        userId = userId,
        couponAlarmSetting = this,
    )
