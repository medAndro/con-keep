package com.conkeep.ui.feature.coupon.edit

import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.conkeep.data.repository.coupon.CouponRepository
import com.conkeep.domain.model.Coupon
import com.conkeep.domain.model.ExpiryDate
import com.conkeep.domain.usecase.coupon.PreLocalProcessCouponUseCase
import com.conkeep.ui.feature.coupon.model.CouponUiModel
import com.conkeep.ui.mapper.toUiModel
import com.conkeep.util.TimeProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel(assistedFactory = CouponEditViewModel.Factory::class)
class CouponEditViewModel
    @AssistedInject
    constructor(
        private val couponRepository: CouponRepository,
        private val timeProvider: TimeProvider,
        private val preLocalProcessCouponUseCase: PreLocalProcessCouponUseCase,
        @Assisted private val couponId: String,
    ) : ViewModel() {
        @AssistedFactory
        interface Factory {
            fun create(couponId: String): CouponEditViewModel
        }

        private var originalCouponUiModel: CouponUiModel? = null
        private var originalDomainCoupon: Coupon? = null

        private val _couponUiModel = MutableStateFlow<CouponUiModel?>(null)
        val couponUiModel = _couponUiModel.asStateFlow()

        private val _toastEvent = MutableSharedFlow<CouponEditEvent>()
        val toastEvent = _toastEvent.asSharedFlow()

        private val _selectedImageUri = MutableStateFlow<Uri?>(null)
        val selectedImageUri = _selectedImageUri.asStateFlow()

        init {
            viewModelScope.launch {
                val domainCoupon = couponRepository.getCouponOnce(couponId)
                originalDomainCoupon = domainCoupon
                val uiModel = domainCoupon?.toUiModel(timeProvider.getToday())

                originalCouponUiModel = uiModel
                _couponUiModel.value = uiModel
            }
        }

        fun isCouponModified(): Boolean =
            when {
                originalCouponUiModel == null -> false
                selectedImageUri.value != null -> true
                else -> couponUiModel.value != originalCouponUiModel
            }

        fun pickCouponImage(uri: Uri) {
            viewModelScope.launch {
                val preProcessResult =
                    preLocalProcessCouponUseCase(uri).getOrElse { e: Throwable ->
                        Log.e("CouponEditViewModel", "쿠폰 이미지 선택 전처리 실패: ${e.message}")
                        return@launch
                    }

                _selectedImageUri.value = preProcessResult.localCachePath?.toUri()
                _couponUiModel.value =
                    couponUiModel.value?.copy(
                        number = preProcessResult.barcode,
                    )
            }
        }

        fun setNewBrandName(string: String) {
            _couponUiModel.value = couponUiModel.value?.copy(brand = string)
        }

        fun setNewProductName(string: String) {
            _couponUiModel.value = couponUiModel.value?.copy(name = string)
        }

        fun setNewPinNumber(string: String) {
            _couponUiModel.value = couponUiModel.value?.copy(number = string)
        }

        fun setNewExpiryDate(expiryDate: ExpiryDate) {
            _couponUiModel.value = couponUiModel.value?.copy(expiryDate = expiryDate)
        }

        fun setNewAmount(amount: Int?) {
            _couponUiModel.value =
                couponUiModel.value?.copy(amount = amount, isMonetary = amount != null)
        }

        fun setNewMemo(string: String) {
            _couponUiModel.value = couponUiModel.value?.copy(memo = string)
        }

        fun saveCouponInfo() {
            if (!isCouponModified()) {
                viewModelScope.launch {
                    _toastEvent.emit(CouponEditEvent.CouponDataIsSame)
                }
                return
            }

            viewModelScope.launch {
                try {
                    originalDomainCoupon?.let {
                        val updatedCoupon =
                            it.copy(
                                brand = couponUiModel.value?.brand,
                                productName = couponUiModel.value?.name,
                                couponPin = couponUiModel.value?.number,
                                expiryDate = couponUiModel.value?.expiryDate ?: ExpiryDate.Empty(),
                                amount = couponUiModel.value?.amount,
                                userMemo = couponUiModel.value?.memo,
                                isMonetary = couponUiModel.value?.isMonetary ?: false,
                            )
                        Log.d("CouponEditViewModel", "saveCouponInfo: $updatedCoupon")
                        couponRepository
                            .update(
                                coupon = updatedCoupon,
                            ).onSuccess {
                                Log.d("CouponEditViewModel", "saveCouponInfo: success")
                                originalDomainCoupon = updatedCoupon
                                originalCouponUiModel = couponUiModel.value?.copy()
                                _toastEvent.emit(CouponEditEvent.CouponEdited)
                            }.onFailure {
                                Log.d("CouponEditViewModel", "saveCouponInfo: fail")
                            }
                    }
                } catch (e: Exception) {
                }
            }
        }
    }

sealed class CouponEditEvent {
    data object CouponEdited : CouponEditEvent()

    data object CouponDataIsSame : CouponEditEvent()
}
