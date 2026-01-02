package com.conkeep.ui.feature.auth

import androidx.activity.ComponentActivity
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.conkeep.navigation.Route
import com.conkeep.ui.feature.auth.component.LoginScreenContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    backStack: NavBackStack<NavKey>,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val activity =
        context as? ComponentActivity
            ?: throw IllegalStateException("Context is not an Activity")

    val isLoggedIn by viewModel.isLoggedIn.collectAsState()

    when (isLoggedIn) {
        true -> {
            backStack.clear()
            backStack.add(Route.CouponScreen)
        }
        false ->
            LoginScreenContent(
                onGoogleSignInClick = { viewModel.signIn(activity) },
            )
    }
}
