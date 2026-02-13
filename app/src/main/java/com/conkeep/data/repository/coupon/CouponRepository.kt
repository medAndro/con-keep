package com.conkeep.data.repository.coupon

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.conkeep.BuildConfig
import com.conkeep.data.auth.SupabaseAuthManager
import com.conkeep.data.local.dao.CouponDao
import com.conkeep.data.local.entity.CouponStatus
import com.conkeep.data.mapper.toDomain
import com.conkeep.data.mapper.toEntity
import com.conkeep.data.remote.dto.AiAnalyzeRequest
import com.conkeep.data.remote.dto.AiCouponResponse
import com.conkeep.data.remote.dto.CouponInfo
import com.conkeep.data.remote.dto.PresignedUrlResponse
import com.conkeep.data.remote.dto.SupabaseCoupon
import com.conkeep.data.remote.dto.toEntity
import com.conkeep.di.annotation.AuthClient
import com.conkeep.di.annotation.R2UploadClient
import com.conkeep.domain.model.Coupon
import com.conkeep.ui.feature.coupon.model.CouponCountSummary
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.get
import io.ktor.client.request.header
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
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@OptIn(ExperimentalCoroutinesApi::class)
class CouponRepository
    @Inject
    constructor(
        private val supabase: SupabaseClient,
        private val couponDao: CouponDao,
        private val authManager: SupabaseAuthManager,
        @param:R2UploadClient private val r2Client: HttpClient,
        @param:AuthClient private val authClient: HttpClient,
    ) {
        fun searchCoupons(
            query: String,
            today: String,
            filterType: Int,
            sortType: Int,
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

        suspend fun addCoupon(coupon: Coupon): String {
            // currentUserIdFlow의 가장 최신 유효 값을 가져옴
            val userId = authManager.currentUserIdFlow.filterNotNull().first()
            couponDao.insert(coupon.copy(userId = userId).toEntity())
            return coupon.id
        }

        suspend fun updateAiRecognitionInfo(
            couponId: String,
            couponInfo: CouponInfo?,
            success: Boolean,
        ) {
            val now = System.currentTimeMillis()
            val status =
                if (success) {
                    CouponStatus.SUCCESS.name
                } else {
                    CouponStatus.AI_FAILED.name
                }
            val finalExpiryDate =
                when {
                    !couponInfo?.expiryDate.isNullOrEmpty() -> {
                        couponInfo.expiryDate.takeIf {
                            runCatching {
                                LocalDate.parse(
                                    it,
                                    DateTimeFormatter.ISO_LOCAL_DATE,
                                )
                            }.isSuccess
                        }
                    }

                    couponInfo?.dday != null -> {
                        LocalDate
                            .now()
                            .plusDays(-couponInfo.dday.toLong())
                            .format(DateTimeFormatter.ISO_LOCAL_DATE)
                    }

                    else -> {
                        null
                    }
                }

            couponDao.updateAiRecognitionInfo(
                couponId = couponId,
                productName = couponInfo?.productName,
                brand = couponInfo?.brand,
                couponPin = couponInfo?.couponPin,
                expiryDate = finalExpiryDate,
                isMonetary = couponInfo?.isMonetary,
                amount = couponInfo?.amount,
                category = couponInfo?.category,
                updatedAt = now,
                status = status,
            )
        }

        suspend fun updateR2Info(
            couponId: String,
            r2Url: String,
            r2Key: String,
        ) {
            couponDao.updateR2Info(couponId, r2Url, r2Key)
        }

        suspend fun getPresignedUrl(
            file: File,
            contentType: String,
        ): Result<PresignedUrlResponse> =
            withContext(Dispatchers.IO) {
                try {
                    val response =
                        authClient.get("${BuildConfig.BASE_URL}/upload-url") {
                            url {
                                parameters.append("ext", file.extension.lowercase())
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

        suspend fun aiCouponRecognizing(
            couponId: String,
            imageUrl: String,
            barcode: String?,
        ): Result<AiCouponResponse> =
            try {
                val response: AiCouponResponse =
                    authClient
                        .post("${BuildConfig.BASE_URL}/analyze") {
                            contentType(ContentType.Application.Json)
                            setBody(
                                AiAnalyzeRequest(
                                    couponId = couponId,
                                    imageUrl = imageUrl,
                                    barcode = barcode,
                                ),
                                // @AuthClient이므로 Bearer 토큰 자동 삽입됨
                            )
                        }.body()

                Result.success(response)
            } catch (e: ClientRequestException) {
                Result.failure(Exception("분석 실패: ${e.response.status}"))
            } catch (e: TimeoutCancellationException) {
                Result.failure(Exception("분석 시간 초과 (네트워크 상태를 확인하세요)"))
            } catch (e: Exception) {
                Result.failure(Exception("분석 중 알 수 없는 오류 발생: ${e.localizedMessage}"))
            }

        suspend fun markAsUsed(
            id: String,
            timestamp: Long,
        ) {
            couponDao.markAsUsed(id, timestamp)
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
