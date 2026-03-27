package com.conkeep.ui.feature.coupon.model

data class CouponCountSummary(
    val counts: Map<CouponFilterType, Int> = emptyMap(),
) {
    fun getCount(type: CouponFilterType): Int = counts[type] ?: 0
}
