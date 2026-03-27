package com.conkeep.ui.feature.auth

import androidx.activity.ComponentActivity
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.conkeep.ui.feature.auth.component.LoginScreenContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val activity =
        context as? ComponentActivity
            ?: throw IllegalStateException("Context is not an Activity")

    val isLoggedIn by viewModel.isLoggedIn.collectAsState()

    when (isLoggedIn) {
        true -> {
            onLoginSuccess()
        }

        false ->
            LoginScreenContent(
                onGoogleSignInClick = { viewModel.signIn(activity) },
            )
    }
}
