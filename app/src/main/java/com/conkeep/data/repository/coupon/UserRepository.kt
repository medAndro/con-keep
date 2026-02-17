// data/repository/coupon/UserRepository.kt
package com.conkeep.data.repository.coupon

import android.util.Log
import com.conkeep.data.remote.dto.DeviceDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock

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
                val device =
                    DeviceDto(
                        userId = userId,
                        fcmToken = fcmToken,
                        lastUsedAt = Clock.System.now().toString(),
                    )

                // onConflict = "fcm_token" 으로 동일 토큰 재등록 방지
                supabase.from("devices").upsert(device) {
                    onConflict = "fcm_token"
                }

                Log.d("UserRepository", "디바이스 등록/갱신 완료: $fcmToken")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e("UserRepository", "디바이스 등록 실패", e)
                Result.failure(e)
            }

        /**
         * 현재 로그인한 유저의 디바이스 FCM토큰을 삭제합니다
         * @param userId 현재 로그인한 유저 ID
         * @param fcmToken 삭제할 FCM 토큰
         */
        suspend fun removeDevice(
            userId: String,
            fcmToken: String,
        ): Result<Unit> =
            try {
                supabase.from("devices").delete {
                    filter {
                        eq("user_id", userId)
                        eq("fcm_token", fcmToken)
                    }
                }

                Log.d("UserRepository", "디바이스 삭제 완료: $fcmToken")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e("UserRepository", "디바이스 삭제 실패", e)
                Result.failure(e)
            }
    }
