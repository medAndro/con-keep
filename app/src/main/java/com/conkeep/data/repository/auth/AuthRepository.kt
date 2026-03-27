package com.conkeep.data.repository.auth

import android.util.Log
import com.conkeep.BuildConfig
import com.conkeep.di.annotation.AuthClient
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.auth.clearAuthTokens
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository
    @Inject
    constructor(
        @param:AuthClient private val authClient: HttpClient,
    ) {
        suspend fun getMasterKey(): MasterKeyResponse = authClient.get("${BuildConfig.BASE_URL}/master-key").body()

        suspend fun removeAccount(): Result<WithdrawResponse> =
            withContext(Dispatchers.IO) {
                try {
                    val response: HttpResponse = authClient.delete("${BuildConfig.BASE_URL}/withdraw")

                    if (response.status.isSuccess()) {
                        authClient.clearAuthTokens()
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
