// data/repository/coupon/UserRepository.kt
package com.conkeep.data.repository.coupon

import android.util.Log
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository
    @Inject
    constructor(
        private val supabase: SupabaseClient,
    ) {
        /**
         * FCM 토큰을 devices 테이블에 등록/갱신합니다.
         * @param userId 사용자 ID (auth.users.id)
         * @param fcmToken FCM 토큰
         */
        suspend fun registerDevice(
            userId: String,
            fcmToken: String,
        ): Result<Unit> =
            try {
                // RPC 호출을 위한 파라미터 맵 구성
                val parameters =
                    mapOf(
                        "p_user_id" to userId,
                        "p_fcm_token" to fcmToken,
                    )

                // upsert 대신 rpc 함수 호출
                supabase.postgrest.rpc("register_device", parameters)

                Log.d("UserRepository", "디바이스 등록/갱신(RPC) 완료: $userId\n$fcmToken")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e("UserRepository", "디바이스 등록 실패", e)
                Result.failure(e)
            }
    }
