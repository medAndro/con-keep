package com.conkeep.ui.feature.coupon.model

data class CouponQueryConfig(
    val query: String = "",
    val filter: CouponFilterType = CouponFilterType.ALL,
    val sort: CouponSortType = CouponSortType.EXPIRY,
)
