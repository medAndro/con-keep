package com.conkeep.ui.feature.coupon.detail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.conkeep.ui.feature.coupon.model.CouponUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CouponDetailViewModel
    @Inject
    constructor(
        private val savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        var couponId by mutableStateOf("")
            private set

        var couponUiModelDetail by mutableStateOf<CouponUiModel?>(null)
            private set

        fun loadCoupon(couponId: String) {
            // 실제 데이터 로드 로직 (여기서는 하드코딩)
            this.couponId = couponId

            couponUiModelDetail =
                when (couponId) {
                    "C001" -> CouponUiModel("C001", "123456789", "스타벅스 아메리카노", "2026-02-01")
                    "C002" -> CouponUiModel("C002", "987654321", "CGV 영화 관람권", "2026-01-15")
                    "C003" -> CouponUiModel("C003", "1111555599", "올리브영 5000원 할인", "2026-03-10")
                    else -> null
                }
        }

        fun useCoupon() {
            println("쿠폰 사용: $couponId")
        }
    }
