package com.conkeep.data.repository.coupon

import android.util.Log
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository
    @Inject
    constructor(
        private val supabase: SupabaseClient,
    ) {
        suspend fun updateFcmToken(
            userId: String,
            token: String,
        ) {
            try {
                supabase
                    .from("profiles")
                    .upsert(mapOf("id" to userId, "fcm_token" to token))
            } catch (e: Exception) {
                Log.e("UserRepository", "FCM 토큰 슈파베이스 업데이트 실패", e)
            }
        }
    }
