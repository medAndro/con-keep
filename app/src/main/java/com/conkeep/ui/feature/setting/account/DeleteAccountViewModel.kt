package com.conkeep.ui.feature.setting.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.conkeep.data.auth.SupabaseAuthManager
import com.conkeep.data.repository.datastore.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class DeleteAccountViewModel
    @Inject
    constructor(
        private val supabaseAuthManager: SupabaseAuthManager,
        private val userPrefs: UserPreferencesRepository,
    ) : ViewModel() {
        fun logout() {
            viewModelScope.launch {
                userPrefs.clearAll()
                supabaseAuthManager.signOut()
            }
        }
    }
