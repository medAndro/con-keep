package com.conkeep.ui.feature.coupon.detail

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel(assistedFactory = CouponDetailViewModel.Factory::class)
class CouponDetailViewModel
    @AssistedInject
    constructor(
        private val couponRepository: CouponRepository,
        private val timeProvider: TimeProvider,
        @Assisted private val couponId: String,
        private val saveCouponUseCase: SaveCouponUseCase,
    ) : ViewModel() {
        @AssistedFactory
        interface Factory {
            fun create(couponId: String): CouponDetailViewModel
        }

        val couponUiModel: StateFlow<CouponUiModel?> =
            couponRepository
                .getCoupon(couponId)
                .map { domainCoupon ->
                    domainCoupon?.toUiModel(timeProvider.getToday())
                }.stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = null,
                )

        private val _errorEvent = MutableSharedFlow<CouponDetailError>()
        val errorEvent = _errorEvent.asSharedFlow()

        fun useCoupon() {
            viewModelScope.launch {
                couponRepository.markAsUsed(
                    id = couponId,
                    timestamp = System.currentTimeMillis(),
                )
            }
        }

        fun unUseCoupon() {
            viewModelScope.launch {
                couponRepository.unUsedMark(
                    id = couponId,
                )
            }
        }

        fun saveCouponImage(onResult: (Boolean?) -> Unit) {
            val currentCoupon = couponUiModel.value ?: return
            viewModelScope.launch {
                val result =
                    saveCouponUseCase(
                        currentCoupon.r2Url,
                        currentCoupon.name,
                        currentCoupon.number,
                    )
                onResult(result)
            }
        }

        fun saveCouponMemo(newMemo: String) {
            if (newMemo == couponUiModel.value?.memo) return

            viewModelScope.launch {
                try {
                    couponRepository.memoSave(
                        id = couponId,
                        memo = newMemo,
                    )
                } catch (e: Exception) {
                    _errorEvent.emit(CouponDetailError.MemoSaveFailed)
                    Log.e("ViewModel", "메모 저장 중 오류 발생", e)
                }
            }
        }

        fun saveCouponAmount(newAmount: String) {
            val newIntAmount = newAmount.toIntOrNull() ?: 0
            val oldIntAmount = couponUiModel.value?.amount?.toIntOrNull() ?: 0
            if (newIntAmount == oldIntAmount) return

            viewModelScope.launch {
                try {
                    couponRepository.amountSave(
                        id = couponId,
                        amount = newIntAmount,
                    )
                } catch (e: Exception) {
                    _errorEvent.emit(CouponDetailError.AmountSaveFailed)
                    Log.e("ViewModel", "금액 저장 중 오류 발생", e)
                }
            }
        }

        fun deleteCoupon(onSuccess: () -> Unit) {
            viewModelScope.launch {
                try {
                    couponRepository
                        .softDelete(
                            id = couponId,
                        ).onSuccess {
                            onSuccess()
                        }.onFailure {
                            _errorEvent.emit(CouponDetailError.SoftDeleteFailed)
                        }
                } catch (e: Exception) {
                    _errorEvent.emit(CouponDetailError.SoftDeleteFailed)
                    Log.e("ViewModel", "쿠폰 삭제 중 오류 발생", e)
                }
            }
        }
    }

sealed interface CouponDetailError {
    data object MemoSaveFailed : CouponDetailError

    data object AmountSaveFailed : CouponDetailError

    data object SoftDeleteFailed : CouponDetailError
}
