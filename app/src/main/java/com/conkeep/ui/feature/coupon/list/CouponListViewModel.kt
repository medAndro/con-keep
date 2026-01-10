package com.conkeep.ui.feature.coupon.list

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.conkeep.data.local.entity.CouponLocalStatus
import com.conkeep.data.mapper.toCouponCategory
import com.conkeep.data.processor.CouponPreProcessResult
import com.conkeep.data.processor.CouponProcessor
import com.conkeep.data.repository.coupon.CouponRepository
import com.conkeep.domain.model.Coupon
import com.conkeep.domain.model.CouponCategory
import com.conkeep.ui.feature.coupon.model.CouponUiModel
import com.conkeep.ui.mapper.toUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.io.File
import java.util.UUID
import javax.inject.Inject
import kotlin.time.Clock

@HiltViewModel
class CouponListViewModel
    @Inject
    constructor(
        private val couponRepository: CouponRepository,
        private val couponProcessor: CouponProcessor,
    ) : ViewModel() {
        val coupons: StateFlow<List<CouponUiModel>> =
            couponRepository
                .getCoupons()
                .map { domainCoupons ->
                    domainCoupons.toUiModel()
                }.stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = emptyList(),
                )

        fun addCouponFromUri(uri: Uri) {
            viewModelScope.launch {
                try {
                    // 1. 전처리
                    val preProcessResult = couponProcessor.preProcessImage(uri)
                    val path = preProcessResult.localPath ?: throw IllegalStateException("로컬 경로 없음")

                    // 2. 순차적 처리 (Fail-Fast)
                    val couponId = addPreCouponToDb(preProcessResult)
                    val urlResponse =
                        couponRepository
                            .getPresignedUrl(
                                File(path),
                                preProcessResult.mimeType ?: "image/jpeg",
                            ).getOrThrow()

                    couponRepository
                        .uploadCouponImageR2(
                            File(path),
                            urlResponse.uploadPresignedUrl,
                            preProcessResult.mimeType ?: "image/jpeg",
                        ).getOrThrow()

                    // 3. 성공 후 업데이트
                    couponRepository.updateR2Info(
                        couponId,
                        urlResponse.imageUrl,
                        urlResponse.r2ObjectKey,
                    )

                    // AI 성공 시 추가 업데이트
                    val aiResponse = couponRepository.aiCouponRecognizing(urlResponse.imageUrl)
                    aiResponse.fold(
                        onSuccess = { response ->
                            // 성공: RECOGNIZED + 쿠폰 정보
                            val finalCouponInfo =
                                response.data.copy(
                                    couponPin =
                                        preProcessResult.barcode.takeUnless { it.isNullOrEmpty() }
                                            ?: response.data.couponPin?.filter { !it.isWhitespace() },
                                    category =
                                        response.data.category
                                            .toCouponCategory()
                                            .name,
                                )
                            couponRepository.updateAiRecognitionInfo(
                                couponId,
                                finalCouponInfo,
                                success = true,
                            )
                        },
                        onFailure = {
                            // 실패: FAILED 상태
                            couponRepository.updateAiRecognitionInfo(
                                couponId,
                                null,
                                success = false,
                            )
                        },
                    )
                } catch (e: Exception) {
                    Log.e("CouponViewModel", "쿠폰 등록 실패: ${e.message}")
                }
            }
        }

        private suspend fun addPreCouponToDb(couponPreProcessResult: CouponPreProcessResult): String {
            val now = Clock.System.now()
            val localDateTime = now.toLocalDateTime(TimeZone.currentSystemDefault())
            val localId = UUID.randomUUID().toString()

            val preCoupon =
                Coupon(
                    id = localId,
                    remoteId = null,
                    userId = "", // supabase user_id
                    imageUrl = null,
                    imageKey = null,
                    thumbnailUrl = null,
                    localImagePath = couponPreProcessResult.localPath,
                    productName = null,
                    brand = null,
                    couponPin = couponPreProcessResult.barcode,
                    expiryDate = null,
                    isMonetary = false,
                    amount = null,
                    category = CouponCategory.ETC,
                    userMemo = null,
                    isUsed = false,
                    usedAt = null,
                    createdAt = localDateTime,
                    updatedAt = localDateTime,
                    isSynced = false,
                    localStatus = CouponLocalStatus.PREPROCESSED.name,
                )

            // Repository 호출 → ID 반환 받음
            couponRepository.addCoupon(preCoupon)
            return localId // 로컬 ID 반환
        }
    }
