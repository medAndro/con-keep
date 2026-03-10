package com.conkeep.ui.feature.setting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.conkeep.data.repository.setting.ExpiryAlertRepository
import com.conkeep.notification.NotificationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class SettingViewModel
    @Inject
    constructor(
        private val notificationHelper: NotificationHelper,
        private val expiryAlertRepository: ExpiryAlertRepository,
    ) : ViewModel() {
        val couponAlarmSettings: Flow<Set<CouponAlarmSetting>> =
            expiryAlertRepository
                .getCouponAlarmSettings()

        private val _toastEvent = MutableSharedFlow<SettingEvent>()
        val toastEvent = _toastEvent.asSharedFlow()

        fun addCouponAlarmSetting(couponAlarmSetting: CouponAlarmSetting) {
            viewModelScope.launch {
                val result = expiryAlertRepository.addCouponAlarmSetting(couponAlarmSetting)

                when (result) {
                    true -> {
                        _toastEvent.emit(SettingEvent.AddCouponAlarmSettingSuccess)
                    }

                    false -> {
                        _toastEvent.emit(SettingEvent.DuplicatedCouponAlarmSetting)
                    }
                }
            }
        }

        fun removeCouponAlarmSetting(couponAlarmSetting: CouponAlarmSetting) {
            viewModelScope.launch {
                val result = expiryAlertRepository.removeCouponAlarmSetting(couponAlarmSetting)
                when (result) {
                    true -> {
                        _toastEvent.emit(SettingEvent.RemoveCouponAlarmSettingSuccess)
                    }

                    false -> {
                        _toastEvent.emit(SettingEvent.RemoveCouponAlarmSettingFailed)
                    }
                }
            }
        }

        fun testNotification() {
            notificationHelper.showExpiryNotification(
                id = 1,
                title = "테스트 알림 제목",
                message = "테스트 알림 내용",
            )
        }
    }

sealed class SettingEvent {
    data object AddCouponAlarmSettingSuccess : SettingEvent()

    data object DuplicatedCouponAlarmSetting : SettingEvent()

    data object RemoveCouponAlarmSettingSuccess : SettingEvent()

    data object RemoveCouponAlarmSettingFailed : SettingEvent()
}
