package com.conkeep.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.conkeep.data.repository.coupon.CouponRepository
import com.conkeep.data.repository.setting.ExpiryAlertRepository
import com.conkeep.ui.feature.setting.notification.CouponAlarmSetting
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.datetime.TimeZone.Companion.currentSystemDefault
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock
import kotlin.time.Instant

@Singleton
class CouponAlarmScheduler
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val couponRepository: CouponRepository,
        private val expiryAlertRepository: ExpiryAlertRepository,
    ) {
        private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        suspend fun scheduleNextAlarm(
            couponAlarmSetting: CouponAlarmSetting,
            fromReceiver: Boolean = false,
        ): Boolean {
            return try {
                // 데이터 확인
                val imminentDate =
                    couponRepository.getNextAlarmTriggerDate(
                        couponAlarmSetting,
                        forceNext = fromReceiver,
                    )
                if (imminentDate == null) {
                    cancelAlarm(couponAlarmSetting)
                    return false // 알람을 등록할 대상이 없으므로 false
                }

                // 시간 계산
                val alarmDate = imminentDate
                val triggerDateTime = alarmDate.atTime(couponAlarmSetting.targetTime)
                val systemTimeZone = currentSystemDefault()
                val triggerAtMillis = triggerDateTime.toInstant(systemTimeZone).toEpochMilliseconds()

                // 유효성 검사 (과거 시간 여부)
                val nowMillis = Clock.System.now().toEpochMilliseconds()
                if (triggerAtMillis <= nowMillis) {
                    Log.d("CouponAlarmScheduler", "계산된 알람 시간이 이미 지났습니다.")
                    return false
                }
                Log.d(
                    "CouponAlarmScheduler",
                    "알람 예약 시간: ${
                        Instant.fromEpochMilliseconds(triggerAtMillis).toLocalDateTime(systemTimeZone)
                    }",
                )

                // PendingIntent 생성
                val intent =
                    Intent(context, CouponAlarmReceiver::class.java).apply {
                        putExtra("daysBefore", couponAlarmSetting.daysBefore)
                        putExtra("targetTime", couponAlarmSetting.targetTime.toNanosecondOfDay())
                    }

                val pendingIntent =
                    PendingIntent.getBroadcast(
                        context,
                        couponAlarmSetting.hashCode(),
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    )

                // 알람 등록 실행
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        // 권한이 있는 경우: 정확한 알람 예약
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerAtMillis,
                            pendingIntent,
                        )
                        Log.d("CouponAlarmScheduler", "정확한 알람 등록 성공: $triggerAtMillis")
                    } else {
                        // 권한이 없는 경우: 일반 알람으로 예약 (차선책)
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerAtMillis,
                            pendingIntent,
                        )
                        Log.d("CouponAlarmScheduler", "정확한 알람 권한 없음: 일반 알람으로 등록")
                    }
                } else {
                    // Android 12 미만: 바로 정확한 알람 사용 가능
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent,
                    )
                }
                true
            } catch (e: Exception) {
                Log.e("CouponAlarmScheduler", "알람 등록 중 오류 발생: ${e.message}")
                false
            }
        }

        fun cancelAlarm(setting: CouponAlarmSetting) {
            val intent = Intent(context, CouponAlarmReceiver::class.java)
            val pendingIntent =
                PendingIntent.getBroadcast(
                    context,
                    setting.hashCode(),
                    intent,
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
                )
            pendingIntent?.let { alarmManager.cancel(it) }
        }

        /**
         * 모든 알람 설정을 DB에서 가져와 현재 시점을 기준으로 재등록합니다.
         */
        suspend fun allAlarmRefresh() {
            try {
                Log.d("CouponAlarmScheduler", "모든 알람 리프레시 시작...")

                // Flow.first() 대신 리포지토리의 단발성 조회 함수 사용
                val currentSettings = expiryAlertRepository.getCouponAlarmSettingsSingleShot()

                if (currentSettings.isEmpty()) {
                    Log.d("CouponAlarmScheduler", "등록된 알람 설정이 없거나 비로그인 상태입니다.")
                    return
                }
                // 각 설정에 대해 알람을 다시 계산하고 등록합니다.
                currentSettings.forEach { setting ->
                    // scheduleNextAlarm 내부에서 이미 알람 계산 및 등록 로직이 처리됩니다.
                    // (기존에 등록된 알람은 동일한 requestCode(hashCode)로 덮어씌워집니다.)
                    val success = scheduleNextAlarm(setting)

                    Log.d("CouponAlarmScheduler", "Setting: ${setting.daysBefore}일 전, 성공여부: $success")
                }
            } catch (e: Exception) {
                Log.e("CouponAlarmScheduler", "모든 알람 리프레시 중 오류 발생: ${e.message}")
            }
        }
    }
