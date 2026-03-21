package com.conkeep.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.conkeep.data.repository.coupon.CouponRepository
import com.conkeep.domain.model.Coupon
import com.conkeep.ui.feature.setting.CouponAlarmSetting
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalTime
import javax.inject.Inject

@AndroidEntryPoint
class CouponAlarmReceiver : BroadcastReceiver() {
    @Inject
    lateinit var couponRepository: CouponRepository

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var couponAlarmScheduler: CouponAlarmScheduler

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        Log.d("CouponAlarmReceiver", "알람이 울립니다.")
        // 전달받은 설정 정보 추출
        val daysBefore = intent.getIntExtra("daysBefore", 0)
        val nanoOfDay = intent.getLongExtra("targetTime", -1L)
        if (nanoOfDay == -1L) return

        val targetTime = LocalTime.fromNanosecondOfDay(nanoOfDay)
        val pendingResult = goAsync() // 브로드캐스트 리시버의 수명을 비동기 작업 동안 연장

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 오늘 알람을 울려야 하는 대상 쿠폰들 조회
                val targetCoupons: List<Coupon> = couponRepository.getImminentCoupons(daysBefore)

                if (targetCoupons.isNotEmpty()) {
                    notificationHelper.showGroupedNotifications(daysBefore, targetCoupons)
                }

                // [핵심] 다음 알람 스케줄링
                // 현재 설정 정보를 다시 세팅 객체로 만들어 스케줄러에 전달
                couponAlarmScheduler.scheduleNextAlarm(
                    CouponAlarmSetting(daysBefore, targetTime),
                    true,
                )
            } catch (e: Exception) {
                Log.e("CouponAlarmReceiver", "알람 처리 중 오류 발생", e)
            } finally {
                pendingResult.finish() // 작업 완료 알림
            }
        }
    }
}
