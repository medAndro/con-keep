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
import com.conkeep.ui.feature.coupon.model.CouponCountHeaderState
import com.conkeep.ui.feature.coupon.model.CouponCountSummary
import com.conkeep.ui.feature.coupon.model.CouponFilterType
import com.conkeep.ui.feature.coupon.model.CouponQueryConfig
import com.conkeep.ui.feature.coupon.model.CouponSortType
import com.conkeep.ui.feature.coupon.model.CouponUiModel
import com.conkeep.ui.mapper.toUiModel
import com.conkeep.util.TimeProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
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
        private val timeProvider: TimeProvider,
    ) : ViewModel() {
        private val _queryConfig = MutableStateFlow(CouponQueryConfig())
        val queryConfig = _queryConfig.asStateFlow()

        private val _searchQueryInput = MutableStateFlow("")
        val searchQueryInput = _searchQueryInput.asStateFlow()

        private val _couponAddedEvent = MutableSharedFlow<String>()
        val couponAddedEvent = _couponAddedEvent.asSharedFlow()

        private val todayIso8601: String = timeProvider.getToday().toString()

        init {
            viewModelScope.launch {
                searchQueryInput
                    .debounce { query ->
                        if (query.isBlank()) 0L else DEBOUNCE_TIMEOUT
                    }.distinctUntilChanged()
                    .collect { debouncedQuery ->
                        _queryConfig.value = _queryConfig.value.copy(query = debouncedQuery)
                    }
            }
        }

        val coupons: Flow<PagingData<CouponUiModel>> =
            _queryConfig
                .flatMapLatest { config: CouponQueryConfig ->
                    couponRepository
                        .searchCoupons(
                            query = config.query,
                            today = todayIso8601,
                            filterType = config.filter.value,
                            sortType = config.sort.value,
                        ).map { pagingData ->
                            pagingData.map { it.toUiModel(today = timeProvider.getToday()) }
                        }
                }.cachedIn(viewModelScope)

        val couponCountHeaderState: StateFlow<CouponCountHeaderState> =
            _queryConfig
                .flatMapLatest { config ->
                    couponRepository
                        .getCouponCount(config.query, todayIso8601, config.filter.value)
                        .map { count ->
                            CouponCountHeaderState(
                                totalCount = count,
                                isSearchActive = config.query.isNotBlank(),
                            )
                        }
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CouponCountHeaderState())
        val couponCountSummary: StateFlow<CouponCountSummary> =
            couponRepository
                .getCouponSummary(todayIso8601)
                .stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(5000),
                    CouponCountSummary(),
                )

        /**
         * 새로 등록된 쿠폰을 보여주기 위한 준비
         */
        fun readyToShowNewCoupon() {
            _searchQueryInput.value = "" // 입력 Flow 즉시 비우기 (디바운스 덮어쓰기 방지)
            _queryConfig.value =
                CouponQueryConfig(
                    query = "",
                    filter = CouponFilterType.ALL,
                    sort = CouponSortType.RECENT,
                )
        }

        /**
         * 검색 키워드만 리셋(필터는 유지)
         */
        fun clearSearchKeyword() {
            _searchQueryInput.value = "" // 입력 Flow 즉시 비우기 (디바운스 덮어쓰기 방지)
            _queryConfig.value =
                queryConfig.value.copy(
                    query = "",
                )
        }

        fun searchCoupons(query: String) {
            _searchQueryInput.value = query
        }

        fun toggleCouponSortType() {
            _queryConfig.value =
                queryConfig.value.copy(
                    sort =
                        when (queryConfig.value.sort) {
                            CouponSortType.RECENT -> CouponSortType.EXPIRY
                            CouponSortType.EXPIRY -> CouponSortType.RECENT
                        },
                )
        }

        fun changeCouponFilterType(filterType: CouponFilterType) {
            _queryConfig.value = queryConfig.value.copy(filter = filterType)
        }

        fun addCouponFromUri(uri: Uri) {
            viewModelScope.launch {
                try {
                    // 1. 전처리
                    val preProcessResult = couponProcessor.preProcessImage(uri)
                    val path = preProcessResult.localPath ?: throw IllegalStateException("로컬 경로 없음")

                    // 2. 순차적 처리 (Fail-Fast)
                    val couponId = addPreCouponToDb(preProcessResult)
                    _couponAddedEvent.emit(couponId)
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
