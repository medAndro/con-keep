package com.conkeep

import androidx.datastore.preferences.core.Preferences
import com.conkeep.data.auth.SupabaseAuthManager
import com.conkeep.data.repository.coupon.UserRepository
import com.conkeep.data.repository.datastore.UserPreferencesRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class FcmTokenRegistrarTest {
    private val supabaseAuthManager = mockk<SupabaseAuthManager>()
    private val userRepository = mockk<UserRepository>()
    private val userPrefs = mockk<UserPreferencesRepository>()
    private val registrar =
        FcmTokenRegistrar(
            supabaseAuthManager = supabaseAuthManager,
            userRepository = userRepository,
            userPrefs = userPrefs,
        )

    @Test
    fun `인증 세션이 없으면 DataStore userId가 있어도 디바이스를 등록하지 않는다`() =
        runTest {
            coEvery { supabaseAuthManager.getAuthenticatedUserId() } returns null
            every { userPrefs.userId } returns flowOf("stale-user-id")

            val result = registrar.registerCurrentDeviceToken("new-fcm-token")

            assertSame(FcmTokenRegistrationResult.AuthRequired, result)
            coVerify(exactly = 0) { userRepository.registerDevice(any(), any()) }
            coVerify(exactly = 0) { userPrefs.updateUserId(any()) }
            coVerify(exactly = 0) { userPrefs.updateFcmToken(any()) }
            verify(exactly = 0) { userPrefs.userId }
        }

    @Test
    fun `인증 세션 userId가 있으면 디바이스 등록 후 로컬 캐시를 갱신한다`() =
        runTest {
            val userId = "authenticated-user-id"
            val token = "new-fcm-token"
            val preferences = mockk<Preferences>()
            coEvery { supabaseAuthManager.getAuthenticatedUserId() } returns userId
            coEvery { userRepository.registerDevice(userId, token) } returns Result.success(Unit)
            coEvery { userPrefs.updateUserId(userId) } returns preferences
            coEvery { userPrefs.updateFcmToken(token) } returns preferences

            val result = registrar.registerCurrentDeviceToken(token)

            assertSame(FcmTokenRegistrationResult.Success, result)
            coVerify { userRepository.registerDevice(userId, token) }
            coVerify { userPrefs.updateUserId(userId) }
            coVerify { userPrefs.updateFcmToken(token) }
        }
}
