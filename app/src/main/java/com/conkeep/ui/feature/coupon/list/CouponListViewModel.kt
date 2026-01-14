package com.conkeep.ui.feature.coupon.list

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.conkeep.data.local.entity.CouponLocalStatus
import com.conkeep.data.mapper.toCouponCategory
import com.conkeep.data.processor.CouponPreProcessResult
import com.conkeep.data.processor.CouponProcessor
import com.conkeep.data.repository.coupon.CouponRepository
import com.conkeep.domain.model.Coupon
import com.conkeep.domain.model.CouponCategory
import com.conkeep.ui.feature.coupon.model.CouponCountSummary
import com.conkeep.ui.feature.coupon.model.CouponFilterType
import com.conkeep.ui.feature.coupon.model.CouponSortType
import com.conkeep.ui.feature.coupon.model.CouponUiModel
import com.conkeep.ui.mapper.toUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.io.File
import java.util.UUID
import javax.inject.Inject
import kotlin.time.Clock

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class CouponListViewModel
    @Inject
    constructor(
        private val couponRepository: CouponRepository,
        private val couponProcessor: CouponProcessor,
    ) : ViewModel() {
        private val _searchQuery = MutableStateFlow("")
        val searchQuery = _searchQuery.asStateFlow()

        private val _couponFilterType = MutableStateFlow(CouponFilterType.ALL)
        val couponFilterType = _couponFilterType.asStateFlow()

        private val _couponSortType = MutableStateFlow(CouponSortType.EXPIRY)
        val couponSortType = _couponSortType.asStateFlow()

        // 오늘 날짜 (ISO 8601 YYYY-MM-DD 형식)
        private val today: String =
            Clock.System
                .now()
                .toLocalDateTime(TimeZone.currentSystemDefault())
                .date
                .toString()

        val coupons: Flow<PagingData<CouponUiModel>> =
            searchQuery
                .debounce(DEBOUNCE_TIMEOUT)
                .distinctUntilChanged()
                .flatMapLatest { query: String ->
                    couponRepository
                        .searchCoupons(query)
                        .map { pagingData ->
                            pagingData.map { it.toUiModel() }
                        }
                }.cachedIn(viewModelScope)

        val couponCount: StateFlow<Int> =
            combine(
                searchQuery.debounce(DEBOUNCE_TIMEOUT).distinctUntilChanged(),
                couponFilterType,
            ) { query, filter ->
                query to filter
            }.flatMapLatest { (query, filter) ->
                couponRepository.getCouponCount(query, today, filter.value)
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

        val couponCountSummary: StateFlow<CouponCountSummary> =
            couponRepository
                .getCouponSummary(today)
                .stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(5000),
                    CouponCountSummary(),
                )

        fun searchCoupons(query: String) {
            _searchQuery.value = query
        }

        fun toggleCouponSortType() {
            _couponSortType.value =
                when (_couponSortType.value) {
                    CouponSortType.RECENT -> CouponSortType.EXPIRY
                    CouponSortType.EXPIRY -> CouponSortType.RECENT
                }
        }

        fun changeCouponFilterType(filterType: CouponFilterType) {
            _couponFilterType.value = filterType
        }

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

        companion object {
            private const val DEBOUNCE_TIMEOUT = 500L
        }
    }
