package com.conkeep.ui.feature.coupon.detail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.conkeep.data.repository.coupon.CouponRepository
import com.conkeep.ui.feature.coupon.model.CouponUiModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel(assistedFactory = CouponDetailViewModel.Factory::class)
class CouponDetailViewModel
    @AssistedInject
    constructor(
        private val couponRepository: CouponRepository,
        @Assisted private val couponId: String,
    ) : ViewModel() {
        @AssistedFactory
        interface Factory {
            fun create(couponId: String): CouponDetailViewModel
        }

        val coupon: StateFlow<CouponUiModel?> =
            couponRepository
                .getCoupon(couponId)
                .map { domainCoupon ->
                    domainCoupon?.let {
                        CouponUiModel(
                            id = it.id,
                            number = it.couponPin ?: "",
                            name = it.productName ?: "이름 없는 쿠폰",
                            expiryDate = it.expiryDate?.toString() ?: "만료일 없음",
                        )
                    }
                }.stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = null,
                )

        fun useCoupon() {
            println("쿠폰 사용: ${coupon.value?.name}")
        }
    }
