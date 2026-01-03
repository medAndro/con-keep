package com.conkeep.ui.feature.coupon

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
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
                val (path, barcode) = couponProcessor.processImage(uri)

                if (path != null) {
                    addDummyCouponToDb(path, barcode)
                }
            }
        }

        private fun addDummyCouponToDb(
            uri: String,
            barcodeNumber: String?,
        ) {
            viewModelScope.launch {
                val now = Clock.System.now()
                val localDateTime = now.toLocalDateTime(TimeZone.currentSystemDefault())
                val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

                val dummyBrands = listOf("스타벅스", "투썸플레이스", "이디야", "CGV", "메가박스", "올리브영", "GS25", "CU")
                val dummyProducts = listOf("아메리카노", "카페라떼", "영화 관람권", "5000원 할인", "음료 교환권", "베이커리 세트")
                val categories = CouponCategory.entries.toTypedArray()

                val randomBrand = dummyBrands.random()
                val randomProduct = dummyProducts.random()

                couponRepository.addCoupon(
                    Coupon(
                        id =
                            Clock.System
                                .now()
                                .toEpochMilliseconds()
                                .toString(),
                        userId = "", // Repository에서 authManager로 자동 설정
                        // 이미지 (더미)
                        imageUrl = "https://via.placeholder.com/400x200?text=$randomBrand",
                        imageKey = "dummy_${Clock.System.now().toEpochMilliseconds()}",
                        thumbnailUrl = "https://via.placeholder.com/100x50?text=$randomBrand",
                        // 로컬 이미지 경로
                        localImagePath = uri,
                        // 쿠폰 정보
                        productName = "$randomBrand $randomProduct",
                        brand = randomBrand,
                        couponPin = barcodeNumber,
                        expiryDate = today.plus(Random.nextInt(7, 90), DateTimeUnit.DAY), // 7~90일 후
                        // 금액 정보
                        isMonetary = Random.nextBoolean(),
                        amount =
                            if (Random.nextBoolean()) {
                                listOf(
                                    1000,
                                    3000,
                                    5000,
                                    10000,
                                ).random()
                            } else {
                                null
                            },
                        // 분류
                        category = categories.random(),
                        userMemo =
                            if (Random.nextBoolean()) {
                                "더미 메모 ${
                                    Random.nextInt(
                                        1,
                                        100,
                                    )
                                }"
                            } else {
                                null
                            },
                        // 사용 정보
                        isUsed = false,
                        usedAt = null,
                        // 메타데이터
                        createdAt = localDateTime,
                        updatedAt = localDateTime,
                        isSynced = false,
                    ),
                )
            }
        }
    }
