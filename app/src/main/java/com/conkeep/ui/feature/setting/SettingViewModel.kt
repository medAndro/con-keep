package com.conkeep.ui.feature.setting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.conkeep.data.repository.setting.ExpiryAlertRepository
import com.conkeep.notification.CouponAlarmScheduler
import com.conkeep.ui.feature.setting.notification.CouponAlarmSetting
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
        private val expiryAlertRepository: ExpiryAlertRepository,
        private val couponAlarmScheduler: CouponAlarmScheduler,
    ) : ViewModel() {
        val couponAlarmSettings: Flow<Set<CouponAlarmSetting>> =
            expiryAlertRepository
                .getCouponAlarmSettings()

        private val _toastEvent = MutableSharedFlow<SettingEvent>()
        val toastEvent = _toastEvent.asSharedFlow()

        fun addCouponAlarmSetting(couponAlarmSetting: CouponAlarmSetting) {
            viewModelScope.launch {
                //  DB에 설정 추가 시도
                val isDbSaved = expiryAlertRepository.addCouponAlarmSetting(couponAlarmSetting)

                if (!isDbSaved) {
                    _toastEvent.emit(SettingEvent.DuplicatedCouponAlarmSetting)
                    return@launch
                }

                // DB 저장 성공 시 알람 스케줄링 시도
                val isAlarmScheduled = couponAlarmScheduler.scheduleNextAlarm(couponAlarmSetting)

                if (isAlarmScheduled) {
                    // 모든 과정 성공
                    _toastEvent.emit(SettingEvent.AddCouponAlarmSettingSuccess)
                }
            }
        }

        fun removeCouponAlarmSetting(couponAlarmSetting: CouponAlarmSetting) {
            viewModelScope.launch {
                val result = expiryAlertRepository.removeCouponAlarmSetting(couponAlarmSetting)
                couponAlarmScheduler.cancelAlarm(couponAlarmSetting)

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
    }

sealed class SettingEvent {
    data object AddCouponAlarmSettingSuccess : SettingEvent()

    data object DuplicatedCouponAlarmSetting : SettingEvent()

    data object RemoveCouponAlarmSettingSuccess : SettingEvent()

    data object RemoveCouponAlarmSettingFailed : SettingEvent()
}
