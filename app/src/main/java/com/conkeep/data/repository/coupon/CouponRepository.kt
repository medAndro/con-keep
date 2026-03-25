package com.conkeep.data.repository.coupon

import android.util.Log
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.conkeep.BuildConfig
import com.conkeep.data.auth.SupabaseAuthManager
import com.conkeep.data.local.dao.CouponDao
import com.conkeep.data.mapper.toDomain
import com.conkeep.data.mapper.toEntity
import com.conkeep.data.remote.dto.AiAnalyzeJobResponse
import com.conkeep.data.remote.dto.AiAnalyzeRequest
import com.conkeep.data.remote.dto.CouponDto
import com.conkeep.data.remote.dto.PresignedUrlResponse
import com.conkeep.data.remote.dto.SupabaseCoupon
import com.conkeep.data.remote.dto.toDto
import com.conkeep.data.remote.dto.toEntity
import com.conkeep.data.repository.datastore.UserPreferencesRepository
import com.conkeep.data.util.CouponCrypto
import com.conkeep.di.annotation.AuthClient
import com.conkeep.di.annotation.R2UploadClient
import com.conkeep.domain.model.Coupon
import com.conkeep.ui.feature.coupon.model.CouponCountSummary
import com.conkeep.ui.feature.coupon.model.CouponSortType
import com.conkeep.ui.feature.setting.notification.CouponAlarmSetting
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.util.cio.readChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock
import kotlin.time.Instant

@Singleton
@OptIn(ExperimentalCoroutinesApi::class)
class CouponRepository
    @Inject
    constructor(
        private val supabase: SupabaseClient,
        private val couponDao: CouponDao,
        private val userPrefs: UserPreferencesRepository,
        private val authManager: SupabaseAuthManager,
        @param:R2UploadClient private val r2Client: HttpClient,
        @param:AuthClient private val authClient: HttpClient,
    ) {
        fun searchCoupons(
            query: String,
            today: String,
            filterType: Int,
            sortType: CouponSortType,
        ): Flow<PagingData<Coupon>> =
            authManager.currentUserIdFlow
                .filterNotNull()
                .flatMapLatest { userId ->
                    Pager(
                        config =
                            PagingConfig(
                                pageSize = 20,
                                enablePlaceholders = false,
                                initialLoadSize = 40,
                            ),
                        initialKey = 0,
                        pagingSourceFactory = {
                            couponDao.searchCouponsPaging(
                                userId = userId,
                                searchQuery = query,
                                today = today,
                                filterType = filterType,
                                sortType = sortType,
                            )
                        },
                    ).flow
                }.map { pagingData ->
                    pagingData.map { it.toDomain() }
                }

        fun getCouponCount(
            query: String,
            today: String,
            filterType: Int,
        ): Flow<Int> =
            authManager.currentUserIdFlow.filterNotNull().flatMapLatest { userId ->
                couponDao.getCouponsCount(userId, query, today, filterType)
            }

        fun getCouponSummary(today: String): Flow<CouponCountSummary> =
            authManager.currentUserIdFlow.filterNotNull().flatMapLatest { userId ->
                couponDao.getCouponSummaryFlow(
                    userId = userId,
                    today = today,
                )
            }

        fun getCoupon(id: String): Flow<Coupon?> =
            couponDao
                .getCouponFlow(id)
                .map { entity -> entity?.toDomain() }

        suspend fun getCouponOnce(id: String): Coupon? =
            couponDao
                .getCouponOnce(id)
                .let { entity -> entity?.toDomain() }

        // N일 만료일인 쿠폰들을 가져옴
        suspend fun getImminentCoupons(daysBefore: Int): List<Coupon> {
            val userId = authManager.getAuthenticatedUserId() ?: return emptyList()

            // 1. 현재 시점 및 타임존 설정
            val now: Instant = Clock.System.now()
            val systemTimeZone: TimeZone = TimeZone.currentSystemDefault()

            // 2. 오늘 날짜 가져오기
            val today: LocalDate = now.toLocalDateTime(systemTimeZone).date

            // 3. daysBefore만큼 더한 날짜 계산
            // 예: 오늘이 2026-03-10이고 daysBefore가 1이면 오늘 기준 만료 1일전인 2026-03-11일 쿠폰들을 찾아 가져옴
            val targetDate = today.plus(daysBefore, DateTimeUnit.DAY)

            // 4. yyyy-mm-dd 스트링으로 변환 (toString()이 기본적으로 이 포맷임)
            val dateString: String = targetDate.toString()

            return couponDao.getImminentCoupons(userId, dateString).toDomain()
        }

        // 전달받은 알람 시간 기준 가장 근미래에 울릴 날짜를 가져옴
        suspend fun getNextAlarmTriggerDate(
            setting: CouponAlarmSetting,
            forceNext: Boolean = false,
        ): LocalDate? {
            val userId = authManager.getAuthenticatedUserId() ?: return null

            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            val today = now.date
            val currentLocalTime = now.time

            // '오늘' 설정된 시간에 알람을 울릴 수 있는지 판단
            val earliestTriggerDate =
                if (forceNext || setting.targetTime < currentLocalTime) {
                    today.plus(1, DateTimeUnit.DAY) // 리시버에서 호출되었거나, 이미 지났다면 내일부터 가능
                } else {
                    today // 아직 안 지났다면 오늘부터 가능
                }

            // 검색해야 할 최소 만료일 계산
            // 예: 3일 전 알람인데 오늘(월) 울리려면, 최소 목요일 만료 쿠폰이 있어야 함
            val thresholdDate = earliestTriggerDate.plus(setting.daysBefore, DateTimeUnit.DAY)

            // DB에서 해당 시점 이후 가장 빠른 만료일 조회
            val nextExpiryDateString =
                couponDao.getMinExpiryDate(userId, thresholdDate.toString()) ?: return null
            val nextExpiryDate = LocalDate.parse(nextExpiryDateString)

            // 실제 알람이 울릴 날짜 계산 (찾은 만료일 - N일 전)
            // 예: 실제 찾은 쿠폰이 금요일 만료라면, 알람은 화요일에 울림 (금 - 3일)
            return nextExpiryDate.minus(setting.daysBefore, DateTimeUnit.DAY)
        }

        suspend fun addCoupon(coupon: Coupon): String {
            // currentUserIdFlow의 가장 최신 유효 값을 가져옴
            val userId = authManager.currentUserIdFlow.filterNotNull().first()
            couponDao.insert(coupon.copy(userId = userId).toEntity())
            return coupon.id
        }

        suspend fun updateR2Info(
            couponId: String,
            r2Url: String,
        ) {
            couponDao.updateR2Info(couponId, r2Url)
        }

        suspend fun getPresignedUrl(
            file: File,
            couponId: String,
            contentType: String,
        ): Result<PresignedUrlResponse> =
            withContext(Dispatchers.IO) {
                try {
                    val response =
                        authClient.get("${BuildConfig.BASE_URL}/upload-url") {
                            url {
                                parameters.append("ext", file.extension.lowercase())
                                parameters.append("couponId", couponId)
                                parameters.append("contentType", contentType)
                                parameters.append("fileSize", file.length().toString())
                            }
                        }

                    if (response.status == HttpStatusCode.OK) {
                        Result.success(response.body<PresignedUrlResponse>())
                    } else {
                        Result.failure(Exception("PresignedUrl 생성 실패: ${response.status}"))
                    }
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

        suspend fun uploadCouponImageR2(
            imageFile: File,
            uploadUrl: String,
            contentType: String,
        ): Result<Unit> =
            withContext(Dispatchers.IO) {
                if (!imageFile.exists()) {
                    return@withContext Result.failure(Exception("파일이 존재하지 않습니다: ${imageFile.absolutePath}"))
                }
                try {
                    val response: HttpResponse =
                        r2Client.put(uploadUrl) {
                            setBody(imageFile.readChannel())
                            contentType(ContentType.parse(contentType))
                            header(HttpHeaders.ContentLength, imageFile.length().toString())
                        }

                    if (response.status.isSuccess()) {
                        Result.success(Unit)
                    } else {
                        Result.failure(Exception("R2 업로드 실패: ${response.status}"))
                    }
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

        suspend fun requestAiAnalyzeJob(
            couponId: String,
            imageUrl: String,
            barcode: String?,
            createAt: String,
        ): Result<String> =
            try {
                val response: AiAnalyzeJobResponse =
                    authClient
                        .post("${BuildConfig.BASE_URL}/analyze") {
                            contentType(ContentType.Application.Json)
                            setBody(AiAnalyzeRequest(couponId, imageUrl, barcode, createAt))
                        }.body()

                if (response.success) {
                    Result.success(response.message)
                } else {
                    Result.failure(Exception("분석 요청 실패 (서버 로직 에러)"))
                }
            } catch (e: ClientRequestException) {
                Result.failure(Exception("분석 요청 실패: ${e.response.status}"))
            } catch (e: TimeoutCancellationException) {
                Result.failure(Exception("분석 요청 시간 초과 (네트워크 상태를 확인하세요)"))
            } catch (e: Exception) {
                Result.failure(Exception("분석 요청 중 알 수 없는 오류 발생: ${e.localizedMessage}"))
            }

        suspend fun updateJob(couponId: String): Result<String> =
            try {
                val couponDto: SupabaseCoupon =
                    couponDao.getCouponOnce(couponId)?.toDto() ?: return Result.failure(
                        Exception("쿠폰을 찾을 수 없습니다."),
                    )

                val response: AiAnalyzeJobResponse =
                    authClient
                        .patch("${BuildConfig.BASE_URL}/update") {
                            contentType(ContentType.Application.Json)
                            setBody(couponDto)
                        }.body()

                if (response.success) {
                    couponDao.setIsClean(couponId)
                    Result.success(response.message)
                } else {
                    Result.failure(Exception("업데이트 요청 실패 (서버 로직 에러)"))
                }
            } catch (e: ClientRequestException) {
                Result.failure(Exception("업데이트 요청 실패: ${e.response.status}"))
            } catch (e: TimeoutCancellationException) {
                Result.failure(Exception("업데이트 요청 시간 초과 (네트워크 상태를 확인하세요)"))
            } catch (e: Exception) {
                Result.failure(Exception("업데이트 요청 중 알 수 없는 오류 발생: ${e.localizedMessage}"))
            }

        suspend fun syncIncremental(): Result<List<CouponDto>> =
            withContext(Dispatchers.IO) {
                try {
                    val lastSyncTime = userPrefs.lastSyncTime.first()
                    val (currentUserId, masterKey) =
                        withTimeout(10000L) {
                            // 10초 동안 대기
                            combine(
                                authManager.currentUserIdFlow,
                                authManager.currentUserMasterKeyFlow,
                            ) { id, key ->
                                if (id != null && key != null) id to key else null
                            }.filterNotNull().first()
                        }

                    val response =
                        supabase.from("coupons").select {
                            filter {
                                gte("updated_at", lastSyncTime)
                                eq("user_id", currentUserId)
                            }
                        }

                    val coupons = response.decodeList<CouponDto>()

                    coupons.forEach { dto: CouponDto ->
                        val localCoupon = couponDao.getCouponById(dto.id)
                        if (localCoupon?.isDirty == true) return@forEach

                        // 복호화 + imageUrl 파생
                        val decryptedPin =
                            dto.couponPin?.let {
                                CouponCrypto.decryptPin(masterKey, it)
                            }
                        val imageKey = CouponCrypto.deriveImageKey(masterKey, dto.id)
                        val imageUrl =
                            "${BuildConfig.COUPON_IMAGE_ENDPOINT}$imageKey.webp" +
                                dto.imageTimestamp?.let { "?t=$it" }.orEmpty()
                        Log.d("CouponRepository", "imageUrl: $imageUrl")

                        val entity =
                            dto.toEntity(imageUrl).copy(
                                couponPin = decryptedPin, // 복호화된 평문
                                imageUrl = imageUrl, // 파생된 URL
                            )
                        couponDao.upsert(entity)
                        couponDao.setIsClean(dto.id)
                        userPrefs.updateLastSyncTime(dto.updatedAt)
                    }
                    Log.d("CouponRepository", "증분 동기화 완료")
                    Result.success(coupons)
                } catch (e: Exception) {
                    Log.e("CouponRepository", "증분 동기화 실패", e)
                    Result.failure(e)
                }
            }

        suspend fun updateStatus(
            id: String,
            couponStatus: String,
        ) {
            couponDao.updateStatus(id, couponStatus)
        }

        suspend fun markAsUsed(
            id: String,
            timestamp: Long,
        ) {
            couponDao.markAsUsed(id, timestamp)
        }

        suspend fun unUsedMark(id: String) {
            couponDao.unUsedMark(id)
        }

        suspend fun memoSave(
            id: String,
            memo: String,
        ) {
            couponDao.memoSave(id, memo)
        }

        suspend fun amountSave(
            id: String,
            amount: Int,
        ) {
            couponDao.amountSave(id, amount.toLong())
        }

        suspend fun update(
            coupon: Coupon,
            isDirty: Boolean = true,
        ): Result<Unit> =
            runCatching {
                val affectedRows = couponDao.update(coupon.toEntity().copy(isDirty = isDirty))
                if (affectedRows > 0) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("수정할 쿠폰을 찾을 수 없습니다."))
                }
            }.getOrElse {
                Result.failure(it) // DB 에러 발생 시
            }

        suspend fun softDelete(id: String): Result<Unit> {
            try {
                couponDao.softDelete(id)
                return Result.success(Unit)
            } catch (e: Exception) {
                return Result.failure(e)
            }
        }

        // 백그라운드에서 Supabase → Room 동기화
        suspend fun syncFromSupabase(): Result<Unit> =
            withContext(Dispatchers.IO) {
                try {
                    val userId =
                        authManager.currentUserIdFlow.first()
                            ?: return@withContext Result.failure(Exception("Not logged in"))

                    val remoteCoupons =
                        supabase
                            .from("coupons")
                            .select {
                                filter { eq("user_id", userId) }
                            }.decodeList<SupabaseCoupon>()

                    couponDao.insertAll(remoteCoupons.map { it.toEntity() })
                    Result.success(Unit)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

        // TODO : 로컬 변경사항 → Supabase 업로드
//        suspend fun syncToSupabase(coupon: Coupon): Result<Unit> =
//            withContext(Dispatchers.IO) {
//                try {
//                    // Supabase에 업로드
//                    supabase
//                        .from("coupons")
//                        .upsert(coupon.toDto())
//
//                    // Room에도 저장 (로컬 캐시)
//                    couponDao.insert(coupon.toEntity())
//
//                    Result.success(Unit)
//                } catch (e: Exception) {
//                    Result.failure(e)
//                }
//            }
    }
