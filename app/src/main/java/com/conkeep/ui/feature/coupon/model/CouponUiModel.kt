package com.conkeep.ui.feature.coupon.model

import com.conkeep.data.local.entity.CouponStatus
import com.conkeep.domain.model.ExpiryDate
import com.conkeep.ui.feature.coupon.list.component.ExpirationBadgeStatus

data class CouponUiModel(
    val id: String,
    val number: String?,
    val name: String?,
    val brand: String?,
    val expiryDate: ExpiryDate,
    val dDay: Int? = null,
    val isUsed: Boolean,
    val isExpired: Boolean,
    val localImagePath: String? = null,
    val r2Url: String? = null,
    val isMonetary: Boolean = false,
    val amount: Int? = null,
    val status: CouponStatus,
    val memo: String = "",
)

val CouponUiModel.badgeStatus: ExpirationBadgeStatus
    get() =
        when {
            dDay == null -> ExpirationBadgeStatus.Common
            dDay < -14 -> ExpirationBadgeStatus.Safe
            dDay < -7 -> ExpirationBadgeStatus.Warning
            dDay <= 0 -> ExpirationBadgeStatus.Expiring
            else -> ExpirationBadgeStatus.Common
        }
