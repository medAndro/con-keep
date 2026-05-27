package com.conkeep.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalTime
import javax.inject.Inject

@AndroidEntryPoint
class CouponAlarmReceiver : BroadcastReceiver() {
    @Inject
    lateinit var couponAlarmHandler: CouponAlarmHandler

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
                couponAlarmHandler.handle(daysBefore, targetTime)
            } catch (e: Exception) {
                Log.e("CouponAlarmReceiver", "알람 처리 중 오류 발생", e)
            } finally {
                pendingResult.finish() // 작업 완료 알림
            }
        }
    }
}
