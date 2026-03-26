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
import androidx.lifecycle.lifecycleScope
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

        // 백그라운드에서 로그인 상태 체크
        lifecycleScope.launch {
            // [초기화 대기] Supabase 초기화 및 세션 복구 대기 (최대 3초)
            withTimeoutOrNull(3000L) {
                authManager.awaitInitialSession()
            } ?: false

            // [상태 감지 시작] 이제부터 로그인 상태 변화를 지속적으로 관찰
            authManager.isLoggedIn.collect { isLoggedIn ->
                when {
                    isLoggedIn -> {
                        Log.d("MainActivity", "로그인 상태 감지: 데이터 동기화 및 경로 설정")

                        // 1. 유저 ID 동기화 (FcmService에서 사용 가능하도록)
                        launch { syncUserIdToPrefs() }

                        // 2. FCM 토큰 업데이트 (변경된 경우에만)
                        launch { handleFcmTokenUpdate() }

                        // 3. 쿠폰 증분 업데이트
                        launch { syncManager.enqueueCouponSync() }

                        // 최초 실행 시에만 초기 경로 설정
                        if (initialRoute.value == null) {
                            initialRoute.value = Route.CouponScreen
                        }
                    }

                    else -> {
                        Log.d("MainActivity", "비로그인 상태 감지")

                        // 초기화가 끝난(isReady가 참이 되려는) 시점 이후에 로그아웃된 경우만 clear
                        // 초기 로딩 중에는 clearAll()을 호출하지 않도록 주의
                        if (isReady.value) {
                            Log.d("MainActivity", "로그아웃 확인됨: 앱을 재시작합니다.")

                            // 1. 로컬 데이터 비우기 (중요: 재시작 직전에 실행)
                            lifecycleScope.launch {
                                userPrefs.clearAll()
                                // 2. 재시작 실행
                                restartApp()
                            }
                            return@collect
                        }

                        if (initialRoute.value == null) {
                            initialRoute.value = Route.LoginScreen
                        }
                    }
                }

                // 초기 경로가 결정되면 스플래시 해제 (딱 한 번만 실행됨)
                if (initialRoute.value != null && !isReady.value) {
                    isReady.value = true
                }
            }
        }
        setContent {
            ConKeepTheme(darkTheme = false) {
                if (isReady.value && initialRoute.value != null) {
                    NavigationRoot(
                        initialRoute = initialRoute.value!!,
                        // [추가] 펜딩된 ID 전달 및 처리가 끝나면 null로 비워주는 콜백
                        pendingCouponId = pendingCouponId.value,
                        onDeepLinkHandled = { pendingCouponId.value = null },
                    )
                }

                // 앱이 처음 켜질 때 Intent 확인
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

            // 4. 비교: 값이 없거나 다르다면 서버 업데이트 진행
            if (currentToken != cachedToken) {
                Log.d("MainActivity", "FCM 토큰 변경 감지: 업데이트를 시작합니다.")
                // 새 토큰 서버 전송
                userRepository.registerDevice(userId, currentToken)

                // 성공적으로 전송 완료 후 로컬 캐시 갱신
                userPrefs.updateFcmToken(currentToken)
                Log.d("MainActivity", "FCM 토큰 서버 업데이트 완료")
            } else {
                Log.d("MainActivity", "FCM 토큰이 동일합니다. 업데이트를 건너뜁니다.")
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "FCM 토큰 업데이트 프로세스 에러", e)
        }
    }
}
