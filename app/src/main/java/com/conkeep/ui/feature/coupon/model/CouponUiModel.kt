package com.conkeep.ui.feature.coupon.model

data class CouponUiModel(
    val id: String,
    val number: String,
    val name: String,
    val expiryDate: String,
    val isUsed: Boolean,
    val localImagePath: String? = null,
)
