package com.conkeep.ui.feature.coupon.model

import com.conkeep.data.local.entity.CouponLocalStatus

data class CouponUiModel(
    val id: String,
    val number: String,
    val name: String,
    val expiryDate: String,
    val isUsed: Boolean,
    val isExpired: Boolean,
    val localImagePath: String? = null,
    val r2Url: String? = null,
    val isMonetary: Boolean = false,
    val amount: Int? = null,
    val localStatus: CouponLocalStatus? = null,
)
