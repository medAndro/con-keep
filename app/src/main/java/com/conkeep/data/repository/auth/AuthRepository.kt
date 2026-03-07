package com.conkeep.data.repository.auth

import com.conkeep.BuildConfig
import com.conkeep.di.annotation.AuthClient
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
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
    }

@Serializable
data class MasterKeyResponse(
    val success: Boolean,
    val masterKey: String,
)
