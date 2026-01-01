package com.conkeep

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.conkeep.ui.auth.AuthViewModel
import com.conkeep.ui.theme.ConKeepTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ConKeepTheme(darkTheme = false) {
                val viewModel: AuthViewModel = hiltViewModel()
                val isLoggedIn = viewModel.isLoggedIn.collectAsState(initial = false)

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    when (isLoggedIn.value) {
                        false ->
                            Greeting(
                                btnText = "ConKeep에 로그인하세요",
                                modifier = Modifier.padding(innerPadding),
                                onSignInClick = { viewModel.signIn(this@MainActivity) },
                            )

                        true ->
                            Greeting(
                                btnText = "${viewModel.getCurrentUser()?.email ?: "이메일없음"}님 안녕하세요",
                                modifier = Modifier.padding(innerPadding),
                                onSignInClick = { viewModel.signIn(this@MainActivity) },
                            )
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(
    btnText: String,
    modifier: Modifier = Modifier,
    onSignInClick: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Button(
            onClick = onSignInClick,
            modifier = modifier,
        ) {
            Text(text = btnText)
        }
    }
}
