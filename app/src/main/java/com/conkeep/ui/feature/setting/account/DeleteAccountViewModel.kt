package com.conkeep.ui.feature.setting.account

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.conkeep.data.auth.AuthEventBus
import com.conkeep.data.repository.auth.AuthRepository
import com.conkeep.data.repository.datastore.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class DeleteAccountViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val userPrefs: UserPreferencesRepository,
        private val authEventBus: AuthEventBus,
    ) : ViewModel() {
        private val _deleteAccountEvent = MutableSharedFlow<DeleteAccountEvent>()
        val deleteAccountEvent = _deleteAccountEvent.asSharedFlow()

        private val _isWithdrawing = MutableStateFlow(false)
        val isWithdrawing = _isWithdrawing.asStateFlow()

        fun withdrawAccount() {
            viewModelScope.launch {
                _isWithdrawing.value = true
                authRepository.removeAccount().fold(
                    onSuccess = {
                        userPrefs.clearAll()
                        authEventBus.emitForceLogout()
                        _isWithdrawing.value = false
                        _deleteAccountEvent.emit(DeleteAccountEvent.WithdrawSuccess)
                    },
                    onFailure = {
                        _isWithdrawing.value = false
                        _deleteAccountEvent.emit(DeleteAccountEvent.WithdrawFail)
                        Log.e("DeleteAccountViewModel", "회원 탈퇴 실패", it)
                    },
                )
            }
        }
    }

sealed class DeleteAccountEvent {
    data object WithdrawSuccess : DeleteAccountEvent()

    data object WithdrawFail : DeleteAccountEvent()
}
