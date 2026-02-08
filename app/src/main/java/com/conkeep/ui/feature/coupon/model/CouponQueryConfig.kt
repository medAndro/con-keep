package com.conkeep.ui.feature.coupon.model

data class CouponQueryConfig(
    val query: String = "",
    val filter: CouponFilterType = CouponFilterType.AVAILABLE,
    val sort: CouponSortType = CouponSortType.EXPIRY,
)
