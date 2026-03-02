package com.conkeep.ui.feature.coupon.list

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import androidx.work.BackoffPolicy
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
import com.conkeep.domain.model.CouponCategory
import com.conkeep.domain.model.ExpiryDate
import com.conkeep.domain.usecase.coupon.PreLocalProcessCouponUseCase
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.Instant

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class CouponListViewModel
    @Inject
    constructor(
        private val couponRepository: CouponRepository,
        private val preLocalProcessCouponUseCase: PreLocalProcessCouponUseCase,
        private val timeProvider: TimeProvider,
        private val workManager: WorkManager,
    ) : ViewModel() {
        private val _queryConfig = MutableStateFlow(CouponQueryConfig())
        val queryConfig = _queryConfig.asStateFlow()

        private val _resetTrigger = MutableStateFlow(0)
        val resetTrigger = _resetTrigger.asStateFlow()

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
            combine(queryConfig, resetTrigger) { config, _ ->
                config
            }.flatMapLatest { config ->
                couponRepository
                    .searchCoupons(
                        query = config.query,
                        today = todayIso8601,
                        filterType = config.filter.value,
                        sortType = config.sort,
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
                    sort = CouponSortType.RECENT_ADD,
                )
            _resetTrigger.value += 1
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
                        when (queryConfig.value.filter) {
                            CouponFilterType.USED -> {
                                when (queryConfig.value.sort) {
                                    CouponSortType.RECENT_USED -> CouponSortType.EXPIRY
                                    CouponSortType.EXPIRY -> CouponSortType.RECENT_USED
                                    else -> CouponSortType.EXPIRY
                                }
                            }

                            else -> {
                                when (queryConfig.value.sort) {
                                    CouponSortType.RECENT_ADD -> CouponSortType.EXPIRY
                                    CouponSortType.EXPIRY -> CouponSortType.RECENT_ADD
                                    else -> CouponSortType.EXPIRY
                                }
                            }
                        },
                )
        }

        fun changeCouponFilterType(filterType: CouponFilterType) {
            _queryConfig.value =
                queryConfig.value.copy(
                    filter = filterType,
                    sort =
                        when (filterType) {
                            CouponFilterType.USED -> CouponSortType.RECENT_USED
                            else -> CouponSortType.EXPIRY
                        },
                )
        }

        fun addCouponFromUri(uri: Uri) {
            viewModelScope.launch {
                try {
                    // 1. 전처리 (로컬 파일 생성 및 바코드 추출)
                    val preProcessResult =
                        preLocalProcessCouponUseCase(uri).getOrElse { e: Throwable ->
                            Log.e("CouponViewModel", "쿠폰 등록 전처리 실패: ${e.message}")
                            return@launch
                        }

                    // 2. 로컬 DB에 '분석 중' 상태로 저장
                    val (couponId, createdInstant) = addPreCouponToDb(preProcessResult)

                    // 3. UI에 즉시 반영 (리스트에 추가)
                    _couponAddedEvent.emit(couponId)

                    // 4. 업로드 및 AI 분석 워커 실행
                    val uploadRequest =
                        OneTimeWorkRequestBuilder<CouponImageUploadWorker>()
                            .setConstraints(
                                Constraints
                                    .Builder()
                                    .setRequiredNetworkType(NetworkType.CONNECTED)
                                    .build(),
                            ).setInputData(
                                workDataOf(
                                    "COUPON_ID" to couponId,
                                    "LOCAL_PATH" to preProcessResult.localCachePath,
                                    "MIME_TYPE" to (preProcessResult.mimeType ?: "image/webp"),
                                    "BARCODE" to preProcessResult.barcode,
                                    "CREATED_AT" to createdInstant.toString(),
                                ),
                            ).setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                            .build()

                    workManager.enqueueUniqueWork(
                        "upload_coupon_$couponId",
                        ExistingWorkPolicy.REPLACE, // 기존에 돌고 있던 워커를 강제로 종료하고 새 워커를 즉시 실행
                        uploadRequest,
                    )
                } catch (e: Exception) {
                    Log.e("CouponViewModel", "쿠폰 등록 초기 실패: ${e.message}")
                }
            }
        }

        private suspend fun addPreCouponToDb(couponPreProcessResult: CouponPreProcessResult): Pair<String, Instant> {
            val nowInstant = Clock.System.now()
            val localId = UUID.randomUUID().toString()
            Log.d("CouponViewModel", "$nowInstant, 로컬 ID: $localId")

            val preCoupon =
                Coupon(
                    id = localId,
                    userId = "", // supabase user_id
                    imageUrl = null,
                    productName = null,
                    brand = null,
                    couponPin = couponPreProcessResult.barcode,
                    expiryDate = ExpiryDate.Processing,
                    isMonetary = false,
                    amount = null,
                    category = CouponCategory.ETC,
                    userMemo = "",
                    isUsed = false,
                    usedAt = null,
                    createdAt = nowInstant,
                    updatedAt = nowInstant,
                    isSynced = false,
                    status = CouponStatus.PENDING.name,
                )

            // Repository 호출 → ID 반환 받음
            couponRepository.addCoupon(preCoupon)
            return localId to nowInstant // 로컬 ID 반환
        }

        companion object {
            private const val DEBOUNCE_TIMEOUT = 500L
        }
    }
