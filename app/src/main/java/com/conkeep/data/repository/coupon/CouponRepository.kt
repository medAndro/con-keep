package com.conkeep.data.repository.coupon

import com.conkeep.data.auth.SupabaseAuthManager
import com.conkeep.data.local.dao.CouponDao
import com.conkeep.data.mapper.toDomain
import com.conkeep.data.mapper.toEntity
import com.conkeep.data.remote.dto.SupabaseCoupon
import com.conkeep.data.remote.dto.toEntity
import com.conkeep.domain.model.Coupon
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CouponRepository
    @Inject
    constructor(
        private val supabase: SupabaseClient,
        private val couponDao: CouponDao,
        private val authManager: SupabaseAuthManager,
    ) {
        fun getCoupons(): Flow<List<Coupon>> =
            couponDao
                .getCouponsFlow(authManager.currentUser?.id ?: "")
                .map { entities -> entities.map { it.toDomain() } }

        suspend fun addCoupon(coupon: Coupon) {
            couponDao.insert(coupon.toEntity())
        }

        // 백그라운드에서 Supabase → Room 동기화
        suspend fun syncFromSupabase(): Result<Unit> =
            withContext(Dispatchers.IO) {
                try {
                    val userId =
                        authManager.currentUser?.id ?: return@withContext Result.failure(
                            Exception("Not logged in"),
                        )

                    // Supabase에서 데이터 가져오기
                    val remoteCoupons =
                        supabase
                            .from("coupons")
                            .select {
                                filter { eq("user_id", userId) }
                            }.decodeList<SupabaseCoupon>()

                    // Room에 저장 (REPLACE 전략으로 자동 업데이트)
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
