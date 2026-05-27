package com.conkeep.data.auth

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Base64
import android.util.Log
import androidx.core.net.toUri
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import com.conkeep.BuildConfig
import com.conkeep.data.repository.auth.AuthRepository
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.SignOutScope
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseAuthManager
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        supabase: SupabaseClient,
        private val authRepository: AuthRepository,
        private val authEventBus: AuthEventBus,
    ) {
        val auth = supabase.auth
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        private val authActionMutex = Mutex()

        init {
            // 객체 생성 시점부터 백그라운드에서 로그아웃 이벤트 구독 시작
            scope.launch {
                authEventBus.logoutEvent.collect {
                    Log.d("SupabaseAuth", "AuthEventBus로부터 전역 로그아웃 신호 수신")

                    signOut(context)

                    // 로컬 DB/DataStore 초기화 필요 시 록직 추가
                }
            }
        }

        // 메모리에 캐싱될 마스터키 (userId와 쌍으로 저장)
        private var cachedMasterKeyInfo: Pair<String, ByteArray>? = null
        private val masterKeyMutex = kotlinx.coroutines.sync.Mutex()

        val isLoggedIn: StateFlow<Boolean> =
            auth.sessionStatus
                .map { status ->
                    when (status) {
                        is SessionStatus.Authenticated -> true
                        is SessionStatus.NotAuthenticated -> false
                        // 그 외 상태(초기화 중, 리프레시 실패)는 null을 반환하여 필터링
                        else -> null
                    }
                }.filterNotNull()
                .stateIn(
                    scope = scope,
                    started = SharingStarted.Eagerly,
                    initialValue = auth.currentSessionOrNull() != null,
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

                // 세션은 있지만 Flow에 아직 ID가 안 채워졌을 수 있으므로 대기
                val userId =
                    withTimeoutOrNull(5000L) {
                        currentUserIdFlow.filterNotNull().first()
                    } ?: auth.currentUserOrNull()?.id

                if (userId == null) {
                    Log.d("SupabaseAuth", "로그인 세션을 확인하지 못했습니다. 작업을 중단합니다.")
                }

                return userId
            } catch (e: Exception) {
                Log.e("SupabaseAuth", "인증 정보 확인 중 오류 발생", e)
                return null
            }
        }

        suspend fun requireAuthenticatedUserId(): AuthUserIdResult {
            try {
                auth.awaitInitialization()

                val userId =
                    withTimeoutOrNull(5000L) {
                        currentUserIdFlow.filterNotNull().first()
                    } ?: auth.currentUserOrNull()?.id

                return if (userId.isNullOrBlank()) {
                    AuthUserIdResult.Unauthenticated
                } else {
                    AuthUserIdResult.Authenticated(userId)
                }
            } catch (e: Exception) {
                Log.e("SupabaseAuth", "인증 정보 확인 중 오류 발생", e)
                return AuthUserIdResult.Unauthenticated
            }
        }

        /**
         * 마스터키를 반환하는 함수 (userId 검증 포함)
         */
        suspend fun getMasterKey(): ByteArray? {
            // 유저가 로그인 상태인지 확인하여 현재 userId 획득
            val currentUserId = getAuthenticatedUserId() ?: return null

            // 캐시가 존재하고, 캐시된 userId가 현재 userId와 정확히 일치하면 바로 반환
            cachedMasterKeyInfo?.let { (cachedUserId, cachedKey) ->
                if (cachedUserId == currentUserId) {
                    return cachedKey
                }
            }

            // 멀티스레드 환경에서 중복 API 호출 방지 (Mutex)
            masterKeyMutex.withLock {
                // 3. 락을 얻고 들어왔을 때 그 사이 다른 스레드가 갱신했을 수 있으므로 재확인
                cachedMasterKeyInfo?.let { (cachedUserId, cachedKey) ->
                    if (cachedUserId == currentUserId) {
                        return cachedKey
                    }
                }

                Log.d("SupabaseAuth", "마스터키 서버에서 페치 시도... (userId: $currentUserId)")
                return try {
                    val response = authRepository.getMasterKey()
                    if (response.success) {
                        val decodedKey = Base64.decode(response.masterKey, Base64.NO_WRAP)

                        //  현재 userId와 페치된 키를 쌍으로 묶어서 캐싱
                        cachedMasterKeyInfo = Pair(currentUserId, decodedKey)

                        Log.d("SupabaseAuth", "마스터키 fetch 성공 및 캐싱 완료")
                        decodedKey
                    } else {
                        Log.e("SupabaseAuth", "마스터키 서버 응답 실패")
                        null
                    }
                } catch (e: Exception) {
                    Log.e("SupabaseAuth", "마스터키 fetch 예외 발생", e)
                    null
                }
            }
        }

        suspend fun awaitInitialSession(): Boolean {
            auth.awaitInitialization()
            return auth.currentSessionOrNull() != null
        }

        suspend fun getValidAccessToken(): String? = authRepository.getValidAccessToken()

        /**
         * 구글 로그인
         * @param activity 호출하는 Activity (Credential Manager UI 표시용)
         */
        suspend fun signInWithGoogle(activity: Activity): Result<UserInfo> =
            authActionMutex.withLock {
                try {
                    Log.d("SupabaseAuth", "Google 로그인 시도")
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

        suspend fun signOut(context: Context) {
            authActionMutex.withLock {
                try {
                    Log.d("SupabaseAuth", "로그아웃 프로세스 시작 (화면 즉시 전환)")

                    cachedMasterKeyInfo = null
                    auth.clearSession()

                    // 앱이 백그라운드로 내려가도 로그아웃
                    withContext(NonCancellable) {
                        try {
                            val credentialManager = CredentialManager.create(context)
                            credentialManager.clearCredentialState(ClearCredentialStateRequest())
                            Log.d("SupabaseAuth", "CredentialManager 세션 초기화 성공")

                            auth.signOut(scope = SignOutScope.GLOBAL)
                            Log.d("SupabaseAuth", "Supabase 글로벌 로그아웃 성공")
                        } catch (e: Exception) {
                            Log.e("SupabaseAuth", "백그라운드 로그아웃 후처리 중 오류", e)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("SupabaseAuth", "로그아웃 중 오류 발생", e)
                    cachedMasterKeyInfo = null
                    auth.clearSession()
                }
            }
        }

        suspend fun refreshSession() {
            val refreshedToken = authRepository.refreshAccessToken()
            if (refreshedToken == null) {
                signOut(context)
            }
        }
    }

sealed interface AuthUserIdResult {
    data class Authenticated(
        val userId: String,
    ) : AuthUserIdResult

    data object Unauthenticated : AuthUserIdResult
}

class NoGoogleAccountException(
    message: String,
) : Exception(message)

class PlayServicesNotReadyException(
    message: String,
) : Exception(message)
