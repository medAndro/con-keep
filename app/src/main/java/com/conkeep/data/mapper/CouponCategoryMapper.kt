package com.conkeep.data.mapper

import com.conkeep.domain.model.CouponCategory

fun String.toCouponCategory(): CouponCategory =
    try {
        CouponCategory.valueOf(this)
    } catch (_: IllegalArgumentException) {
        CouponCategory.ETC
    }
