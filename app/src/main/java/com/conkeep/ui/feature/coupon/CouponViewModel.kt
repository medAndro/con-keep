package com.conkeep.ui.feature.coupon

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.conkeep.data.processor.CouponPreProcessResult
import com.conkeep.data.processor.CouponProcessor
import com.conkeep.data.remote.dto.PresignedUrlResponse
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
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import java.io.File
import java.util.UUID
import javax.inject.Inject
import kotlin.random.Random
import kotlin.time.Clock

@HiltViewModel
class CouponViewModel
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

        fun addDummyCouponFromUri(uri: Uri) {
            viewModelScope.launch {
                // 1. 이미지 프로세싱 (MIME 타입, 로컬 경로, 바코드 추출)
                val preProcessResult = couponProcessor.preProcessImage(uri)
                val path = preProcessResult.localPath ?: return@launch
                val mimeType = preProcessResult.mimeType ?: "image/jpeg"

                val couponId = addDummyCouponToDb(preProcessResult)

                // 2. 백엔드에 Presigned URL 요청
                val urlResponse: PresignedUrlResponse =
                    couponRepository
                        .getPresignedUrl(
                            file = File(path),
                            contentType = mimeType,
                        ).getOrNull() ?: return@launch

                // 3. R2에 실제 업로드 실행
                val uploadResult =
                    couponRepository.uploadCouponImageR2(
                        imageFile = File(path),
                        uploadUrl = urlResponse.uploadPresignedUrl,
                        contentType = mimeType,
                    )

                // 4. 업로드 성공 시에만 실제 imageUrl을 담아 DB 저장
                uploadResult
                    .onSuccess {
                        updateR2Info(couponId, urlResponse.imageUrl, urlResponse.r2ObjectKey)
                    }.onFailure { println("로그: 업로드 실패 - ${it.message}") }
            }
        }

        private fun updateR2Info(
            couponId: String,
            imageUrl: String,
            imageKey: String,
        ) {
            viewModelScope.launch {
                couponRepository.updateR2Info(couponId, imageUrl, imageKey)
            }
        }

        private suspend fun addDummyCouponToDb(couponPreProcessResult: CouponPreProcessResult): String {
            val now = Clock.System.now()
            val localDateTime = now.toLocalDateTime(TimeZone.currentSystemDefault())
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

            val dummyBrands = listOf("스타벅스", "투썸플레이스", "이디야", "CGV", "메가박스", "올리브영", "GS25", "CU")
            val dummyProducts = listOf("아메리카노", "카페라떼", "영화 관람권", "5000원 할인", "음료 교환권", "베이커리 세트")
            val categories = CouponCategory.entries.toTypedArray()

            val randomBrand = dummyBrands.random()
            val randomProduct = dummyProducts.random()

            val localId = UUID.randomUUID().toString()

            val dummyCoupon =
                Coupon(
                    id = localId,
                    remoteId = null,
                    userId = "",
                    imageUrl = null,
                    imageKey = null,
                    thumbnailUrl = null,
                    localImagePath = couponPreProcessResult.localPath,
                    productName = "$randomBrand $randomProduct",
                    brand = randomBrand,
                    couponPin = couponPreProcessResult.barcode,
                    expiryDate = today.plus(Random.nextInt(7, 90), DateTimeUnit.DAY),
                    isMonetary = Random.nextBoolean(),
                    amount = if (Random.nextBoolean()) listOf(1000, 3000, 5000, 10000).random() else null,
                    category = categories.random(),
                    userMemo = if (Random.nextBoolean()) "더미 메모 ${Random.nextInt(1, 100)}" else null,
                    isUsed = false,
                    usedAt = null,
                    createdAt = localDateTime,
                    updatedAt = localDateTime,
                    isSynced = false,
                )

            // Repository 호출 → ID 반환 받음
            couponRepository.addCoupon(dummyCoupon)
            return localId // 로컬 ID 반환
        }
    }
