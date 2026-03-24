package com.conkeep.data.auth

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.Settings
import android.util.Base64
import android.util.Log
import androidx.core.net.toUri
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import com.conkeep.BuildConfig
import com.conkeep.data.repository.auth.AuthRepository
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock

@Singleton
class SupabaseAuthManager
    @Inject
    constructor(
        supabase: SupabaseClient,
        authRepository: AuthRepository,
    ) {
        val auth = supabase.auth
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        // 로그인 상태 (StateFlow)
        val isLoggedIn: StateFlow<Boolean> =
            auth.sessionStatus
                .map { status ->
                    status is SessionStatus.Authenticated
                }.stateIn(
                    scope = scope,
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

        /**
         * [중요] 백그라운드 작업용 안전한 ID 획득 함수
         * 1. Supabase 초기화 대기 (로컬 세션 복구)
         * 2. 로그인 기록이 없으면 null 반환 (불필요한 동작 방지)
         * 3. 로그인 기록이 있다면 유효한 ID가 나올 때까지 최대 5초 대기
         */
        suspend fun getAuthenticatedUserId(): String? {
            try {
                // Supabase 내부 초기화 (DataStore에서 토큰 읽기) 대기
                auth.awaitInitialization()

                // 초기화 직후 세션이 아예 없다면 비로그인 상태로 간주
                if (auth.currentSessionOrNull() == null) {
                    Log.d("SupabaseAuth", "로그인 세션이 없습니다. 작업을 중단합니다.")
                    return null
                }

                // 세션은 있지만 Flow에 아직 ID가 안 채워졌을 수 있으므로 대기
                return withTimeoutOrNull(5000L) {
                    currentUserIdFlow.filterNotNull().first()
                }
            } catch (e: Exception) {
                Log.e("SupabaseAuth", "인증 정보 확인 중 오류 발생", e)
                return null
            }
        }

        // 현재 사용자의 마스터키
        val currentUserMasterKeyFlow: StateFlow<ByteArray?> =
            auth.sessionStatus
                .map { status ->
                    when (status) {
                        is SessionStatus.Authenticated -> {
                            runCatching { authRepository.getMasterKey().masterKey }
                                .onFailure { Log.e("SupabaseAuth", "마스터키 fetch 실패", it) }
                                .onSuccess { Log.d("SupabaseAuth", "마스터키 fetch 성공") }
                                .getOrNull()
                                ?.let { Base64.decode(it, Base64.NO_WRAP) }
                        }

                        else -> null
                    }
                }.stateIn(scope, SharingStarted.Eagerly, null)

        suspend fun awaitInitialSession(): Boolean {
            auth.awaitInitialization()
            return auth.currentSessionOrNull() != null
        }

        suspend fun getValidAccessToken(): String? {
            val session: UserSession = auth.currentSessionOrNull() ?: return null

            // 만료 2분 전에 미리 갱신 시도
            val expiresIn = (session.expiresAt - Clock.System.now()).inWholeSeconds
            Log.d("SupabaseAuth", "토큰 만료 시간: $expiresIn")
            return if (expiresIn < 120) {
                try {
                    auth.refreshCurrentSession()
                    auth.currentSessionOrNull()?.accessToken
                } catch (e: Exception) {
                    Log.e("SupabaseAuth", "토큰 갱신 실패", e)
                    null
                }
            } else {
                session.accessToken
            }
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
            } catch (e: GetCredentialProviderConfigurationException) {
                // Play Services 미준비
                handlePlayServicesUpdate(activity)
                Result.failure(PlayServicesNotReadyException("Google Play Services를 업데이트해주세요"))
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

        private fun handlePlayServicesUpdate(activity: Activity) {
            val api = GoogleApiAvailability.getInstance()
            val status = api.isGooglePlayServicesAvailable(activity)

            if (status != ConnectionResult.SUCCESS && api.isUserResolvableError(status)) {
                api.getErrorDialog(activity, status, 9000)?.show()
            } else {
                try {
                    activity.startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            "market://details?id=com.google.android.gms".toUri(),
                        ),
                    )
                } catch (e: ActivityNotFoundException) {
                    activity.startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            "https://play.google.com/store/apps/details?id=com.google.android.gms".toUri(),
                        ),
                    )
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

class PlayServicesNotReadyException(
    message: String,
) : Exception(message)
