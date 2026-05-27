package com.conkeep

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.work.WorkManager
import com.conkeep.data.auth.SupabaseAuthManager
import com.conkeep.data.repository.coupon.UserRepository
import com.conkeep.data.repository.datastore.UserPreferencesRepository
import com.conkeep.data.sync.SyncManager
import com.conkeep.navigation.NavigationRoot
import com.conkeep.navigation.Route
import com.conkeep.ui.theme.ConKeepTheme
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.AndroidEntryPoint
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var authManager: SupabaseAuthManager

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var userPrefs: UserPreferencesRepository

    @Inject
    lateinit var workManager: WorkManager

    @Inject
    lateinit var syncManager: SyncManager

    private var isReady = mutableStateOf(false)
    private val initialRoute = mutableStateOf<Route?>(null)
    private var pendingCouponId = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()

        enableEdgeToEdge(
            statusBarStyle =
                SystemBarStyle.light(
                    scrim = Color.TRANSPARENT,
                    darkScrim = Color.TRANSPARENT,
                ),
            navigationBarStyle =
                SystemBarStyle.light(
                    scrim = Color.TRANSPARENT,
                    darkScrim = Color.TRANSPARENT,
                ),
        )

        super.onCreate(savedInstanceState)

        splashScreen.setKeepOnScreenCondition {
            !isReady.value
        }

        var lastHandledUserId: String? = null
        // 백그라운드에서 로그인 상태 체크
        lifecycleScope.launch {
            // [초기화 대기]
            withTimeoutOrNull(3000L) {
                authManager.awaitInitialSession()
            } ?: false

            // 화면에 보일 때(STARTED 이상)만 Flow를 수집합니다.
            // 백그라운드(WorkManager 등)에서 상태가 변해도 화면을 띄우지 않습니다.
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                authManager.auth.sessionStatus.collect { status ->
                    when (status) {
                        SessionStatus.Initializing -> {
                            Log.d("MainActivity", "세션 초기화 중...")
                        }

                        is SessionStatus.Authenticated -> {
                            Log.d("MainActivity", "로그인 상태 감지")

                            // 중복 호출 방지 로직
                            val currentUserId = authManager.getAuthenticatedUserId()
                            if (currentUserId != null && lastHandledUserId != currentUserId) {
                                lastHandledUserId = currentUserId

                                launch {
                                    syncUserIdToPrefs()
                                    handleFcmTokenUpdate()
                                }
                                launch { syncManager.enqueueCouponSync() }
                            }

                            if (initialRoute.value == null) {
                                initialRoute.value = Route.CouponScreen
                            }
                            if (!isReady.value) isReady.value = true
                        }

                        is SessionStatus.RefreshFailure -> {
                            Log.w("MainActivity", "리프레시 실패 (일시적 네트워크 오류 등). 기존 뷰 유지")
                            if (initialRoute.value == null) {
                                initialRoute.value = Route.CouponScreen
                            }
                            if (!isReady.value) isReady.value = true
                        }

                        is SessionStatus.NotAuthenticated -> {
                            Log.d("MainActivity", "비로그인 상태 감지. isSignOut: ${status.isSignOut}")
                            lastHandledUserId = null // 유저 초기화

                            // 앱 초기 실행 시점이 비로그인일 경우
                            if (initialRoute.value == null) {
                                initialRoute.value = Route.LoginScreen
                                if (!isReady.value) isReady.value = true
                                return@collect
                            }

                            // 명시적 로그아웃(탈퇴 등)일 때만 재시작
                            // repeatOnLifecycle 안장이므로 앱이 백그라운드일 때는 실행되지 않습니다.
                            if (status.isSignOut && isReady.value) {
                                Log.d("MainActivity", "명시적 로그아웃 확인됨: 앱 재시작")
                                lifecycleScope.launch {
                                    userPrefs.clearAll()
                                    restartApp()
                                }
                            }
                        }
                    }
                }
            }
        }

        setContent {
            ConKeepTheme(darkTheme = false) {
                if (isReady.value && initialRoute.value != null) {
                    NavigationRoot(
                        initialRoute = initialRoute.value!!,
                        pendingCouponId = pendingCouponId.value,
                        onDeepLinkHandled = { pendingCouponId.value = null },
                    )
                }
                LaunchedEffect(Unit) {
                    handleIntent(intent)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    /**
     * 앱을 완전히 깨끗한 상태로 재시작합니다.
     */
    private fun restartApp() {
        val intent =
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        startActivity(intent)
        finish()
    }

    private fun handleIntent(intent: Intent) {
        val couponId = intent.getStringExtra("coupon_id")
        val navigateTo = intent.getStringExtra("navigate_to")

        if (navigateTo == "detail" && couponId != null) {
            // 이제 navController.navigate 대신 상태값을 변경합니다.
            pendingCouponId.value = couponId
        }
    }

    /**
     * 유저 ID를 Datastore에 동기화합니다.
     * 이는 FcmService가 앱이 꺼진 상태에서도 누구의 신호인지 알게 하기 위함입니다.
     */
    private suspend fun syncUserIdToPrefs() {
        try {
            val userId = authManager.getAuthenticatedUserId() ?: return
            userPrefs.updateUserId(userId)
            Log.d("MainActivity", "유저 ID 로컬 동기화 완료: $userId")
        } catch (e: Exception) {
            Log.e("MainActivity", "유저 ID 동기화 실패", e)
        }
    }

    /**
     * FCM 토큰 업데이트 로직
     */
    private suspend fun handleFcmTokenUpdate() {
        try {
            // 1. 유저 ID 대기 (최대 5초)
            val userId = authManager.getAuthenticatedUserId() ?: return

            // 2. 현재 기기의 최신 토큰 가져오기
            val currentToken = FirebaseMessaging.getInstance().token.await()

            // 3. 로컬에 저장된 이전 토큰 확인
            val cachedToken = userPrefs.fcmToken.first() ?: ""
            val cachedUserId = userPrefs.userId.first() ?: ""

            // 4. 비교: 값이 없거나 다르다면 서버 업데이트 진행
            if (currentToken != cachedToken || userId != cachedUserId) {
                Log.d("MainActivity", "FCM 토큰 변경 감지: 업데이트를 시작합니다.")
                // 새 토큰 서버 전송
                userRepository
                    .registerDevice(userId, currentToken)
                    .onSuccess {
                        // 성공적으로 전송 완료 후 로컬 캐시 갱신
                        userPrefs.updateUserId(userId)
                        userPrefs.updateFcmToken(currentToken)
                        Log.d("MainActivity", "FCM 토큰 서버 업데이트 완료")
                    }.onFailure {
                        Log.e("MainActivity", "FCM 토큰 서버 업데이트 실패", it)
                    }
            } else {
                Log.d("MainActivity", "FCM 토큰이 동일합니다. 업데이트를 건너뜁니다.")
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "FCM 토큰 업데이트 프로세스 에러", e)
        }
    }
}
