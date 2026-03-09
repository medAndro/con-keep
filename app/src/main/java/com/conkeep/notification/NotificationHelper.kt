package com.conkeep.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.conkeep.R
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
                        EXPIRY_REMINDER_ID,
                        "만료일 알림",
                        NotificationManager.IMPORTANCE_HIGH,
                    ).apply { description = "쿠폰 만료 전 발송되는 알림" },
                    NotificationChannel(
                        COUPON_ETC_INFO_ID,
                        "쿠폰 정보 알림",
                        NotificationManager.IMPORTANCE_DEFAULT,
                    ).apply { description = "만료일 알림을 제외한 쿠폰 정보 알림" },
                    NotificationChannel(
                        NOTICE_ID,
                        "공지사항 알림",
                        NotificationManager.IMPORTANCE_DEFAULT,
                    ).apply { description = "서비스 공지사항 알림" },
                )
            manager.createNotificationChannels(channels)
        }

        // 공지사항 알림 전송 함수
        fun showNotifyNotification(
            id: Int,
            title: String,
            message: String,
        ) {
            val notification =
                NotificationCompat
                    .Builder(context, NOTICE_ID)
                    .setSmallIcon(R.drawable.ic_corn_ms_emoji)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)
                    .build()

            manager.notify(id, notification)
        }

        // 만료일 알림 전송 함수
        fun showExpiryNotification(
            id: Int,
            title: String,
            message: String,
        ) {
            val notification =
                NotificationCompat
                    .Builder(context, EXPIRY_REMINDER_ID)
                    .setSmallIcon(R.drawable.ic_corn_ms_emoji)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .build()

            manager.notify(id, notification)
        }

        companion object {
            private const val EXPIRY_REMINDER_ID = "expiry_reminder_id"
            private const val COUPON_ETC_INFO_ID = "coupon_etc_info_id"
            private const val NOTICE_ID = "notice_id"
        }
    }
