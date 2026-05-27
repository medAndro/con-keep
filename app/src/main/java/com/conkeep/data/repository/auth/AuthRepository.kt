package com.conkeep.data.repository.auth

import android.util.Log
import com.conkeep.BuildConfig
import com.conkeep.di.annotation.PlainAuthClient
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.user.UserSession
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock

@Singleton
class AuthRepository
    @Inject
    constructor(
        @param:PlainAuthClient private val authClient: HttpClient,
        private val supabase: SupabaseClient,
    ) {
        suspend fun currentAccessToken(): String? = supabase.auth.currentAccessTokenOrNull()

        suspend fun getValidAccessToken(): String? {
            val session: UserSession = supabase.auth.currentSessionOrNull() ?: return null
            val expiresIn = (session.expiresAt - Clock.System.now()).inWholeSeconds

            return if (expiresIn < TOKEN_REFRESH_THRESHOLD_SECONDS) {
                refreshAccessToken()
            } else {
                session.accessToken
            }
        }

        suspend fun refreshAccessToken(): String? =
            try {
                supabase.auth.refreshCurrentSession()
                supabase.auth.currentAccessTokenOrNull()
            } catch (e: Exception) {
                Log.e("AuthRepository", "토큰 갱신 실패", e)
                null
            }

        suspend fun getMasterKey(): MasterKeyResponse =
            authClient
                .get("${BuildConfig.BASE_URL}/master-key") {
                    getValidAccessToken()?.let { token ->
                        header(HttpHeaders.Authorization, "Bearer $token")
                    }
                }.body()

        suspend fun removeAccount(): Result<WithdrawResponse> =
            withContext(Dispatchers.IO) {
                try {
                    val response: HttpResponse =
                        authClient.delete("${BuildConfig.BASE_URL}/withdraw") {
                            getValidAccessToken()?.let { token ->
                                header(HttpHeaders.Authorization, "Bearer $token")
                            }
                        }

                    if (response.status.isSuccess()) {
                        val body = response.body<WithdrawResponse>()
                        Result.success(body)
                    } else {
                        Result.failure(Exception("회원 탈퇴 실패: ${response.status}"))
                    }
                } catch (e: Exception) {
                    Log.e("AuthRepository", "회원 탈퇴 중 오류 발생", e)
                    Result.failure(e)
                }
            }

        companion object {
            private const val TOKEN_REFRESH_THRESHOLD_SECONDS = 120
        }
    }

@Serializable
data class MasterKeyResponse(
    val success: Boolean,
    val masterKey: String,
)

@Serializable
data class WithdrawResponse(
    val success: Boolean,
    val message: String,
)
