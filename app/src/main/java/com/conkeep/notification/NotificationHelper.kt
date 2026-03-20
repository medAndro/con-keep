package com.conkeep.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.conkeep.R
import com.conkeep.domain.model.Coupon
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper
    @Inject
    constructor(
        private val context: Context,
    ) {
        private val manager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 앱 시작 시 호출할 채널 생성 함수
        fun createAllChannels() {
            val channels =
                listOf(
                    NotificationChannel(
                        EXPIRY_REMINDER_CHANNEL_ID,
                        "만료일 알림",
                        NotificationManager.IMPORTANCE_HIGH,
                    ).apply { description = "쿠폰 만료 전 발송되는 알림" },
                    NotificationChannel(
                        COUPON_ETC_INFO_ID,
                        "쿠폰 정보 알림",
                        NotificationManager.IMPORTANCE_DEFAULT,
                    ).apply { description = "만료일 알림을 제외한 쿠폰 정보 알림" },
                    NotificationChannel(
                        NOTICE_CHANNEL_ID,
                        "공지사항 알림",
                        NotificationManager.IMPORTANCE_DEFAULT,
                    ).apply { description = "서비스 공지사항 알림" },
                )
            manager.createNotificationChannels(channels)
        }

        // 공지사항 알림 전송 함수
        fun showNotifyNotification(
            title: String,
            message: String,
        ) {
            val notification =
                NotificationCompat
                    .Builder(context, NOTICE_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_corn_ms_emoji)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)
                    .build()

            manager.notify(NOTICE_ID, notification)
        }

        // 만료일 알림 전송 함수
        fun showGroupedNotifications(coupons: List<Coupon>) {
            // 개별 쿠폰 알림 생성 및 발송
            coupons.forEach { coupon ->
                val notification =
                    NotificationCompat
                        .Builder(context, EXPIRY_REMINDER_CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_corn_ms_emoji)
                        .setContentTitle("쿠폰 만료 임박")
                        .setContentText("${coupon.productName}이(가) 곧 만료됩니다!")
                        .setGroup(EXPIRY_GROUP_KEY_COUPON)
                        .setAutoCancel(true)
                        .build()

                manager.notify(coupon.id.hashCode(), notification)
            }

            // 그룹 요약(Summary) 알림 생성 및 발송
            val summaryNotification =
                NotificationCompat
                    .Builder(context, EXPIRY_REMINDER_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_corn_ms_emoji)
                    .setContentTitle("만료 예정 쿠폰")
                    .setContentText("${coupons.size}개의 쿠폰이 만료될 예정입니다.")
                    .setSubText("쿠폰 알림") // 알림 상단에 표시될 작은 텍스트
                    .setGroup(EXPIRY_GROUP_KEY_COUPON)
                    .setGroupSummary(true)
                    .setAutoCancel(true)
                    .build()

            // 요약 알림은 고정된 ID를 사용하여 하나만 유지되게 합니다.
            manager.notify(EXPIRY_REMINDER_ID, summaryNotification)
        }

        companion object {
            private const val EXPIRY_REMINDER_CHANNEL_ID = "expiry_reminder"
            private const val EXPIRY_REMINDER_ID = 1001

            private const val EXPIRY_GROUP_KEY_COUPON = "com.conkeep.COUPON_EXPIRY"
            private const val COUPON_ETC_INFO_ID = "coupon_etc_info_id"
            private const val NOTICE_CHANNEL_ID = "notice"
            private const val NOTICE_ID = 1002
        }
    }
