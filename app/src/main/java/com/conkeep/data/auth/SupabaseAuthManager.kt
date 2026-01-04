package com.conkeep.data.auth

import android.app.Activity
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.conkeep.BuildConfig
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseAuthManager
    @Inject
    constructor(
        supabase: SupabaseClient,
    ) {
        val auth = supabase.auth

        // 로그인 상태 (StateFlow)
        val isLoggedIn: StateFlow<Boolean> =
            auth.sessionStatus
                .map { status ->
                    status is SessionStatus.Authenticated
                }.stateIn(
                    scope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
                    started = SharingStarted.Eagerly, // 앱 시작부터 추적
                    initialValue = false,
                )

        // 현재 사용자 정보
        val currentUser: UserInfo?
            get() = auth.currentUserOrNull()

        val accessToken: String?
            get() = auth.currentSessionOrNull()?.accessToken

        suspend fun awaitInitialSession(): Boolean =
            auth.sessionStatus
                .filter { it !is SessionStatus.Initializing } // 초기화 완료까지 대기
                .first()
                .let { it is SessionStatus.Authenticated }

        suspend fun awaitInitialSessionV2(): Boolean {
            auth.awaitInitialization()
            return auth.currentSessionOrNull() != null
        }

        /**
         * 구글 로그인
         * @param activity 호출하는 Activity (Credential Manager UI 표시용)
         */
        suspend fun signInWithGoogle(activity: Activity): Result<UserInfo> =
            try {
                // 1. Google Credential Manager 설정
                val credentialManager = CredentialManager.create(activity)

                val googleIdOption =
                    GetGoogleIdOption
                        .Builder()
                        .setFilterByAuthorizedAccounts(false) // 모든 계정 표시
                        .setServerClientId(BuildConfig.WEB_CLIENT_ID)
                        .build()

                val request =
                    GetCredentialRequest
                        .Builder()
                        .addCredentialOption(googleIdOption)
                        .build()

                // 2. 인증 요청 및 토큰 획득
                val result =
                    credentialManager.getCredential(
                        request = request,
                        context = activity,
                    )

                val credential = GoogleIdTokenCredential.createFrom(result.credential.data)
                val idToken = credential.idToken

                // 3. Supabase Auth에 ID Token 전달 (회원가입/로그인 동시 처리)
                auth.signInWith(IDToken) {
                    this.idToken = idToken
                    provider = Google
                }

                // 4. 사용자 정보 반환
                val user =
                    auth.currentUserOrNull()
                        ?: throw Exception("로그인 후 사용자 정보를 가져오지 못했습니다.")

                Result.success(user)
            } catch (e: Exception) {
                // 사용자가 취소한 경우 등에 대한 예외 처리 필요
                Result.failure(e)
            }

        suspend fun signOut() {
            auth.signOut()
        }

        suspend fun refreshSession() {
            try {
                auth.refreshCurrentSession()
            } catch (e: Exception) {
                signOut()
            }
        }
    }
