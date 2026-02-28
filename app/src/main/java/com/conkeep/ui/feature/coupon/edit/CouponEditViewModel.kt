package com.conkeep.ui.feature.coupon.edit

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.conkeep.data.repository.coupon.CouponRepository
import com.conkeep.domain.model.ExpiryDate
import com.conkeep.ui.feature.coupon.model.CouponUiModel
import com.conkeep.ui.mapper.toUiModel
import com.conkeep.util.TimeProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel(assistedFactory = CouponEditViewModel.Factory::class)
class CouponEditViewModel
    @AssistedInject
    constructor(
        private val couponRepository: CouponRepository,
        private val timeProvider: TimeProvider,
        @Assisted private val couponId: String,
    ) : ViewModel() {
        @AssistedFactory
        interface Factory {
            fun create(couponId: String): CouponEditViewModel
        }

        private var initialCoupon: CouponUiModel? = null
        private val _coupon = MutableStateFlow<CouponUiModel?>(null)
        val coupon = _coupon.asStateFlow()

        private val _selectedImageUri = MutableStateFlow<Uri?>(null)
        val selectedImageUri = _selectedImageUri.asStateFlow()

        init {
            viewModelScope.launch {
                val domainCoupon = couponRepository.getCouponOnce(couponId)
                val uiModel = domainCoupon?.toUiModel(timeProvider.getToday())

                initialCoupon = uiModel
                _coupon.value = uiModel
            }
        }

        fun isCouponModifiedChecker(): Boolean =
            when {
                initialCoupon == null -> false
                selectedImageUri.value != null -> true
                else -> coupon.value != initialCoupon
            }

        fun pickCouponImage(uri: Uri) {
            _selectedImageUri.value = uri
        }

        fun setNewBrandName(string: String) {
            _coupon.value = coupon.value?.copy(brand = string)
        }

        fun setNewProductName(string: String) {
            _coupon.value = coupon.value?.copy(name = string)
        }

        fun setNewPinNumber(string: String) {
            _coupon.value = coupon.value?.copy(number = string)
        }

        fun setNewExpiryDate(expiryDate: ExpiryDate) {
            _coupon.value = coupon.value?.copy(expiryDate = expiryDate)
        }

        fun setNewAmount(amount: Int?) {
            _coupon.value = coupon.value?.copy(amount = amount)
        }

        fun setNewMemo(string: String) {
            _coupon.value = coupon.value?.copy(memo = string)
        }

        fun useCoupon() {
            viewModelScope.launch {
                couponRepository.markAsUsed(
                    id = couponId,
                    timestamp = System.currentTimeMillis(),
                )
            }
        }
    }
