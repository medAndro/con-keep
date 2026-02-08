package com.conkeep.ui.feature.coupon.model

import com.conkeep.data.local.entity.CouponLocalStatus
import com.conkeep.ui.feature.coupon.list.component.ExpirationBadgeStatus
import kotlinx.datetime.LocalDate

data class CouponUiModel(
    val id: String,
    val number: String,
    val name: String,
    val brand: String,
    val expiryDate: LocalDate?,
    val dDay: Int? = null,
    val isUsed: Boolean,
    val isExpired: Boolean,
    val localImagePath: String? = null,
    val r2Url: String? = null,
    val isMonetary: Boolean = false,
    val amount: Int? = null,
    val localStatus: CouponLocalStatus? = null,
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
