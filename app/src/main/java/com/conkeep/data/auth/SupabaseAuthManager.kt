package com.conkeep.data.auth

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.Settings
import android.util.Log
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
import kotlinx.coroutines.flow.distinctUntilChanged
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
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

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

        // 현재 사용자 정보 Flow
        val currentUserFlow: StateFlow<UserInfo?> =
            auth.sessionStatus
                .map { status ->
                    if (status is SessionStatus.Authenticated) {
                        status.session.user
                    } else {
                        auth.currentUserOrNull()
                    }
                }.distinctUntilChanged()
                .stateIn(
                    scope = scope,
                    started = SharingStarted.Eagerly,
                    initialValue = auth.currentUserOrNull(),
                )

        // 현재 사용자 ID Flow
        val currentUserIdFlow: StateFlow<String?> =
            currentUserFlow
                .map { it?.id }
                .stateIn(scope, SharingStarted.Eagerly, auth.currentUserOrNull()?.id)

        // 액세스 토큰 Flow
        val accessTokenFlow: StateFlow<String?> =
            auth.sessionStatus
                .map { status ->
                    if (status is SessionStatus.Authenticated) {
                        status.session.accessToken
                    } else {
                        auth.currentSessionOrNull()?.accessToken
                    }
                }.distinctUntilChanged()
                .stateIn(scope, SharingStarted.Eagerly, auth.currentSessionOrNull()?.accessToken)

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
                        .setAutoSelectEnabled(true)
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
            } catch (e: androidx.credentials.exceptions.NoCredentialException) {
                promptAddGoogleAccount(activity)
                Result.failure(NoGoogleAccountException("구글 계정을 먼저 추가해주세요"))
            } catch (e: androidx.credentials.exceptions.GetCredentialCancellationException) {
                Result.failure(e)
            } catch (e: Exception) {
                Log.e("SupabaseAuth", "Google 로그인 실패", e)
                Result.failure(e)
            }

        /**
         * 구글 계정 추가 화면 열기
         */
        private fun promptAddGoogleAccount(activity: Activity) {
            try {
                val intent =
                    Intent(Settings.ACTION_ADD_ACCOUNT).apply {
                        // 구글 계정만 표시
                        putExtra(Settings.EXTRA_ACCOUNT_TYPES, arrayOf("com.google"))
                    }
                activity.startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                // 일부 기기에서 ACTION_ADD_ACCOUNT를 지원하지 않을 수 있음
                Log.e("SupabaseAuth", "계정 추가 화면을 열 수 없습니다", e)
                // 폴백: 동기화 설정 화면
                try {
                    activity.startActivity(Intent(Settings.ACTION_SYNC_SETTINGS))
                } catch (e2: Exception) {
                    Log.e("SupabaseAuth", "설정 화면을 열 수 없습니다", e2)
                }
            }
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

class NoGoogleAccountException(
    message: String,
) : Exception(message)
