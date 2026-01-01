package com.conkeep.ui.feature.coupon

import androidx.lifecycle.ViewModel
import com.conkeep.ui.feature.coupon.model.Coupon
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CouponViewModel
    @Inject
    constructor() : ViewModel() {
        val coupons =
            listOf(
                Coupon("C001", "123456789", "스타벅스 아메리카노", "2026-02-01"),
                Coupon("C002", "987654321", "CGV 영화 관람권", "2026-01-15"),
                Coupon("C003", "1111555599", "올리브영 5000원 할인", "2026-03-10"),
            )
    }
