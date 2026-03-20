package com.conkeep.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    @Inject
    lateinit var couponAlarmScheduler: CouponAlarmScheduler

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        // 시스템으로부터 전달받은 액션이 부팅 완료인지 확인
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            Log.d("BootReceiver", "기기 부팅 완료 감지: 알람 재등록을 시작합니다.")

            // 비동기 작업을 위해 리시버 수명 연장
            val pendingResult = goAsync()

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // 이전에 만들어둔 전체 리프레시 함수 호출
                    // 내부에서 authManager.getAuthenticatedUserId()를 호출하여
                    // 세션 복구를 기다린 후 안전하게 알람을 다시 잡습니다.
                    couponAlarmScheduler.allAlarmRefresh()

                    Log.d("BootReceiver", "모든 알람 재등록 완료")
                } catch (e: Exception) {
                    Log.e("BootReceiver", "알람 복구 중 오류 발생", e)
                } finally {
                    // 작업 종료 알림
                    pendingResult.finish()
                }
            }
        }
    }
}
