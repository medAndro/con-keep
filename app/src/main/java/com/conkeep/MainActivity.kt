package com.conkeep

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.conkeep.data.auth.SupabaseAuthManager
import com.conkeep.navigation.NavigationRoot
import com.conkeep.navigation.Route
import com.conkeep.ui.theme.ConKeepTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var authManager: SupabaseAuthManager

    private var isReady = mutableStateOf(false)
    private val initialRoute = mutableStateOf<Route?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        splashScreen.setKeepOnScreenCondition {
            !isReady.value
        }

        // 백그라운드에서 로그인 상태 체크
        lifecycleScope.launch {
            delay(100) // 최소 표시 시간

            val isLoggedIn = authManager.awaitInitialSessionV2()

            Log.d("MainActivity", "세션 상태: $isLoggedIn")
            Log.d("MainActivity", "사용자: ${authManager.currentUser?.email}")

            initialRoute.value =
                when {
                    isLoggedIn -> Route.CouponScreen
                    else -> Route.LoginScreen
                }

            isReady.value = true
        }

        setContent {
            ConKeepTheme(darkTheme = false) {
                if (isReady.value && initialRoute.value != null) {
                    NavigationRoot(initialRoute = initialRoute.value!!)
                }
            }
        }
    }
}
