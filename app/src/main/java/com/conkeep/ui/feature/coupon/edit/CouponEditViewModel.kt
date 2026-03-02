package com.conkeep.ui.feature.coupon.edit

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.conkeep.data.local.entity.CouponStatus
import com.conkeep.data.processor.CouponPreProcessResult
import com.conkeep.data.repository.coupon.CouponRepository
import com.conkeep.data.worker.CouponImageUploadWorker
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
        private val workManager: WorkManager,
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

        private val _selectedImageUriStatus =
            MutableStateFlow<SelectedImageUriStatus>(SelectedImageUriStatus.Init)
        val selectedImageUriStatus = _selectedImageUriStatus.asStateFlow()

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
                couponUiModel.value != originalCouponUiModel -> true
                else -> {
                    when (selectedImageUriStatus.value) {
                        is SelectedImageUriStatus.Selected -> true
                        else -> false
                    }
                }
            }

        fun pickCouponImage(uri: Uri) {
            viewModelScope.launch {
                val preProcessResult: CouponPreProcessResult =
                    preLocalProcessCouponUseCase(uri).getOrElse { e: Throwable ->
                        Log.e("CouponEditViewModel", "쿠폰 이미지 선택 전처리 실패: ${e.message}")
                        return@launch
                    }

                if (preProcessResult.localCacheAbsolutePath != null) {
                    _selectedImageUriStatus.value =
                        SelectedImageUriStatus.Selected(preProcessResult.localCacheAbsolutePath)
                }

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
                                brand = couponUiModel.value?.brand ?: "",
                                productName = couponUiModel.value?.name ?: "",
                                couponPin = couponUiModel.value?.number ?: "",
                                expiryDate = couponUiModel.value?.expiryDate ?: ExpiryDate.Empty(),
                                amount = couponUiModel.value?.amount ?: 0,
                                userMemo = couponUiModel.value?.memo ?: "",
                                isMonetary = couponUiModel.value?.isMonetary ?: false,
                                status = CouponStatus.SUCCESS.name,
                                imageUrl =
                                    selectedImageUriStatus.value.let { selectedImageUriStatus: SelectedImageUriStatus ->
                                        when (selectedImageUriStatus) {
                                            is SelectedImageUriStatus.Selected -> selectedImageUriStatus.localAbsolutePath
                                            is SelectedImageUriStatus.Uploaded -> selectedImageUriStatus.localAbsolutePath
                                            else -> couponUiModel.value?.r2Url
                                        }
                                    },
                            )
                        Log.d("CouponEditViewModel", "saveCouponInfo: $updatedCoupon")
                        couponRepository
                            .update(
                                coupon = updatedCoupon,
                                isDirty = true,
                            ).onSuccess {
                                Log.d("CouponEditViewModel", "saveCouponInfo: success")
                                originalDomainCoupon = updatedCoupon
                                originalCouponUiModel = couponUiModel.value?.copy()
                                if (selectedImageUriStatus.value is SelectedImageUriStatus.Selected) {
                                    val localAbsolutePath =
                                        (selectedImageUriStatus.value as SelectedImageUriStatus.Selected).localAbsolutePath
                                    _selectedImageUriStatus.value =
                                        SelectedImageUriStatus.Uploaded(localAbsolutePath)
                                    uploadImageWorker((localAbsolutePath))
                                }
                                _toastEvent.emit(CouponEditEvent.CouponEdited)
                            }.onFailure { e: Throwable ->
                                Log.e("CouponEditViewModel", "saveCouponInfo: fail ${e.message}")
                            }
                    }
                } catch (e: Exception) {
                    Log.e("CouponEditViewModel", "saveCouponInfo: fail ${e.message}")
                }
            }
        }

        fun uploadImageWorker(localAbsolutePath: String) {
            val uploadRequest =
                OneTimeWorkRequestBuilder<CouponImageUploadWorker>()
                    .setConstraints(
                        Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build(),
                    ).setInputData(
                        workDataOf(
                            "COUPON_ID" to couponId,
                            "LOCAL_ABSOLUTE_PATH_STRING" to localAbsolutePath,
                            "UPLOAD_IMAGE_ONLY" to true,
                        ),
                    ).build()

            workManager
                .beginUniqueWork(
                    "upload_img_update_coupon_$couponId",
                    ExistingWorkPolicy.REPLACE,
                    uploadRequest,
                ).enqueue()
        }
    }

sealed class CouponEditEvent {
    data object CouponEdited : CouponEditEvent()

    data object CouponDataIsSame : CouponEditEvent()
}

sealed class SelectedImageUriStatus {
    data object Init : SelectedImageUriStatus()

    data class Selected(
        val localAbsolutePath: String,
    ) : SelectedImageUriStatus()

    data class Uploaded(
        val localAbsolutePath: String,
    ) : SelectedImageUriStatus()
}
