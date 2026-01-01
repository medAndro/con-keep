package com.conkeep.ui.feature.coupon.detail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.conkeep.ui.feature.coupon.model.Coupon
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CouponDetailViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        // SavedStateHandle에서 직접 id 가져오기
        val couponId: String = savedStateHandle.get<String>("id") ?: ""

        var couponDetail by mutableStateOf<Coupon?>(null)
            private set

        fun loadCoupon(couponId: String) {
            // 실제 데이터 로드 로직 (여기서는 하드코딩)
            couponDetail =
                when (couponId) {
                    "C001" -> Coupon("C001", "123456789", "스타벅스 아메리카노", "2026-02-01")
                    "C002" -> Coupon("C002", "987654321", "CGV 영화 관람권", "2026-01-15")
                    "C003" -> Coupon("C003", "1111555599", "올리브영 5000원 할인", "2026-03-10")
                    else -> null
                }
        }

        fun useCoupon() {
            println("쿠폰 사용: $couponId")
        }
    }
