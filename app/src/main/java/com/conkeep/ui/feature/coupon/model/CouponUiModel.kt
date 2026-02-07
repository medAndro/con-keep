package com.conkeep.ui.feature.coupon.model

import com.conkeep.data.local.entity.CouponLocalStatus
import kotlinx.datetime.LocalDate

data class CouponUiModel(
    val id: String,
    val number: String,
    val name: String,
    val brand: String,
    val expiryDate: LocalDate?,
    val isUsed: Boolean,
    val isExpired: Boolean,
    val localImagePath: String? = null,
    val r2Url: String? = null,
    val isMonetary: Boolean = false,
    val amount: Int? = null,
    val localStatus: CouponLocalStatus? = null,
)
