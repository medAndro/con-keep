package com.conkeep

import android.util.Log
import androidx.work.WorkManager
import com.conkeep.data.repository.coupon.UserRepository
import com.conkeep.data.repository.datastore.UserPreferencesRepository
import com.conkeep.data.sync.SyncManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 이 클래스는 구글 플레이 서비스와 직접 통신하는 특수한 서비스입니다.
 * 앱이 꺼져 있거나 백그라운드에 있어도 시스템이 이 클래스를 깨워 신호를 전달합니다.
 */
@AndroidEntryPoint
class FcmService : FirebaseMessagingService() {
    /**
     * 1. 의존성 주입: Hilt를 사용하여 UserRepository를 가져옵니다.
     * 서비스는 시스템이 생성하므로 생성자 주입이 안 되어 @Inject lateinit을 사용합니다.
     */
    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var userPrefs: UserPreferencesRepository

    @Inject
    lateinit var workManager: WorkManager

    @Inject
    lateinit var syncManager: SyncManager

    /**
     * 2. 서비스 전용 코루틴 스코프
     * SupervisorJob: 자식 작업 중 하나가 실패해도 전체 스코프가 취소되지 않게 방어합니다.
     * Dispatchers.IO: 네트워크나 DB 작업에 최적화된 스레드를 사용합니다.
     */
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * 3. onMessageReceived
     * 구글 서버에서 푸시 신호가 오면 시스템이 이 함수를 가장 먼저 실행합니다.
     */
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        Log.d(TAG, "수신처: ${message.from}")

        // Data 페이로드 확인: 서버가 보낸 '보이지 않는 데이터'가 있는지 확인합니다.
        if (message.data.isNotEmpty()) {
            Log.d(TAG, "메시지 데이터: ${message.data}")

            when (message.data["type"]) {
                "SYNC_TRIGGER" -> {
                    // 서버가 "동기화" 신호를 보낸 경우입니다.
                    val timestamp = message.data["timestamp"]
                    handleSyncTrigger(timestamp)
                }
            }
        }

        // Notification: 상단 알림 팝업 정보 (포그라운드 상태일 때만 여기서 처리 가능)
        message.notification?.let {
            Log.d(TAG, "알림 제목: ${it.title}, 내용: ${it.body}")
        }
    }

    /**
     * 4. onNewToken
     * 구글이 내 폰의 주소(FCM Token)를 새로 발급했을 때 호출됩니다.
     * 이때 즉시 서버에 보고하지 않으면 서버는 내 폰을 찾을 수 없게 됩니다.
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "새로운 토큰 발급됨: $token")

        // 주소가 바뀌었으니 즉시 서버 장부를 업데이트합니다.
        sendTokenToServer(token)
    }

    /**
     * 5. 실제 동기화 신호 처리 로직
     */
    private fun handleSyncTrigger(timestamp: String?) {
        Log.d(TAG, "동기화 트리거 작동 시점: $timestamp")
        syncManager.enqueueCouponSync()
    }

    /**
     * 6. 서버로 토큰 전송 (주소 보고)
     * 비동기 코루틴 안에서 유저 ID를 찾아 서버(Supabase)에 주소를 등록합니다.
     */
    private fun sendTokenToServer(token: String) {
        serviceScope.launch {
            try {
                // 이 서비스는 화면이 없으므로, 로컬 저장소(Datastore)에서 현재 로그인된 유저 ID를 찾습니다.
                val userId =
                    userPrefs.userId.first() ?: run {
                        Log.w(TAG, "유저 ID를 찾을 수 없어 토큰 업데이트를 건너뜁니다.")
                        return@launch
                    }

                // 서버(Supabase)의 profiles 테이블에 내 주소를 저장합니다.
                userRepository.updateFcmToken(userId, token)

                // 나중에 중복 요청을 방지하기 위해 로컬 캐시에도 저장해 둡니다.
                userPrefs.updateFcmToken(token)

                Log.d(TAG, "서버 토큰 업데이트 성공")
            } catch (e: Exception) {
                Log.e(TAG, "토큰 업데이트 중 오류 발생", e)
            }
        }
    }

    companion object {
        private const val TAG = "FcmService"
    }
}
