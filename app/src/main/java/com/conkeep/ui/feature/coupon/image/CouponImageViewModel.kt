package com.conkeep.ui.feature.coupon.image

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.conkeep.data.local.file.LocalFileManager
import com.conkeep.data.repository.coupon.CouponRepository
import com.conkeep.ui.feature.coupon.model.CouponUiModel
import com.conkeep.ui.mapper.toUiModel
import com.conkeep.util.TimeProvider
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
@HiltViewModel(assistedFactory = CouponImageViewModel.Factory::class)
class CouponImageViewModel
    @AssistedInject
    constructor(
        private val couponRepository: CouponRepository,
        private val timeProvider: TimeProvider,
        @Assisted private val couponId: String,
        private val fileManager: LocalFileManager,
    ) : ViewModel() {
        @AssistedFactory
        interface Factory {
            fun create(couponId: String): CouponImageViewModel
        }

        val coupon: StateFlow<CouponUiModel?> =
            couponRepository
                .getCoupon(couponId)
                .map { domainCoupon ->
                    domainCoupon?.toUiModel(timeProvider.getToday())
                }.stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = null,
                )

        fun getShareUri(): Uri? {
            val path = coupon.value?.localImagePath ?: return null
            val name = "${coupon.value?.name}_${coupon.value?.number}"
            return fileManager.getShareUriWithCustomName(path, name)
        }

        fun saveCoupon() {
        }
    }
