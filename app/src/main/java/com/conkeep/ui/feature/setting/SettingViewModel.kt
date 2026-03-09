package com.conkeep.ui.feature.setting

import androidx.lifecycle.ViewModel
import com.conkeep.notification.NotificationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import javax.inject.Inject

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class SettingViewModel
    @Inject
    constructor(
        private val notificationHelper: NotificationHelper,
    ) : ViewModel() {
        fun testNotification() {
            notificationHelper.showExpiryNotification(
                id = 1,
                title = "테스트 알림 제목",
                message = "테스트 알림 내용",
            )
        }
    }
