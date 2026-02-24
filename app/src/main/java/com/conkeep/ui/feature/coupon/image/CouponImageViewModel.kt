package com.conkeep.ui.feature.coupon.image

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.conkeep.data.local.file.LocalFileManager
import com.conkeep.data.repository.coupon.CouponRepository
import com.conkeep.domain.usecase.coupon.SaveCouponUseCase
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel(assistedFactory = CouponImageViewModel.Factory::class)
class CouponImageViewModel
    @AssistedInject
    constructor(
        private val couponRepository: CouponRepository,
        private val timeProvider: TimeProvider,
        @Assisted private val couponId: String,
        private val fileManager: LocalFileManager,
        private val saveCouponUseCase: SaveCouponUseCase,
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

        fun shareCoupon(onResult: (Uri?) -> Unit) {
            val currentCoupon = coupon.value ?: return
            val path = currentCoupon.localImagePath ?: return
            val name = "${coupon.value?.name}_${coupon.value?.number}"

            viewModelScope.launch {
                val shareUri = fileManager.getShareUriWithCustomName(path, name)
                onResult(shareUri)
            }
        }

        fun saveCouponImage(onResult: (Boolean?) -> Unit) {
            val currentCoupon = coupon.value ?: return
            viewModelScope.launch {
                val result =
                    saveCouponUseCase(
                        currentCoupon.localImagePath,
                        currentCoupon.name,
                        currentCoupon.number,
                    )
                onResult(result)
            }
        }
    }
