package com.conkeep

import com.conkeep.data.auth.SupabaseAuthManager
import com.conkeep.data.repository.coupon.UserRepository
import com.conkeep.data.repository.datastore.UserPreferencesRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FcmTokenRegistrar
    @Inject
    constructor(
        private val supabaseAuthManager: SupabaseAuthManager,
        private val userRepository: UserRepository,
        private val userPrefs: UserPreferencesRepository,
    ) {
        suspend fun registerCurrentDeviceToken(token: String): FcmTokenRegistrationResult {
            val userId =
                supabaseAuthManager.getAuthenticatedUserId()
                    ?: return FcmTokenRegistrationResult.AuthRequired

            return userRepository
                .registerDevice(userId, token)
                .fold(
                    onSuccess = {
                        userPrefs.updateUserId(userId)
                        userPrefs.updateFcmToken(token)
                        FcmTokenRegistrationResult.Success
                    },
                    onFailure = { FcmTokenRegistrationResult.Failure(it) },
                )
        }
    }

sealed interface FcmTokenRegistrationResult {
    data object Success : FcmTokenRegistrationResult

    data object AuthRequired : FcmTokenRegistrationResult

    data class Failure(
        val throwable: Throwable,
    ) : FcmTokenRegistrationResult
}
