package com.conkeep.ui.feature.auth

import android.app.Activity
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.conkeep.data.auth.SupabaseAuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel
    @Inject
    constructor(
        private val authManager: SupabaseAuthManager,
    ) : ViewModel() {
        // 로그인 상태를 관찰할 수 있는 Flow
        val isLoggedIn = authManager.isLoggedIn

        fun getCurrentUser() = authManager.currentUser

        fun signIn(activity: Activity) {
            viewModelScope.launch {
                val result = authManager.signInWithGoogle(activity)
                result
                    .onSuccess { user ->
                        Toast.makeText(activity, "${user.email} 로그인 성공", Toast.LENGTH_SHORT).show()
                    }.onFailure { error ->
                        Toast.makeText(activity, "${error.message} 로그인 실패", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }
