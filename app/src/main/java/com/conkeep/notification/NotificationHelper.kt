package com.conkeep.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.conkeep.MainActivity
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
        fun showGroupedNotifications(
            daysBefore: Int,
            coupons: List<Coupon>,
        ) {
            // 개별 쿠폰 알림 생성 및 발송
            coupons.forEach { coupon ->

                val intent =
                    Intent(context, MainActivity::class.java).apply {
                        // 앱이 이미 켜져 있을 때 기존 Activity를 재사용하도록 설정
                        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        // 전달할 데이터 추가
                        putExtra("coupon_id", coupon.id)
                        // 상세 페이지로 가야 함을 알리는 플래그 (선택 사항)
                        putExtra("navigate_to", "detail")
                    }

                // PendingIntent 생성 (Android 12+ 대응을 위해 FLAG_IMMUTABLE 사용)
                val pendingIntent =
                    PendingIntent.getActivity(
                        context,
                        coupon.id.hashCode(), // 각 쿠폰마다 고유한 RequestCode 필요
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    )

                val notification =
                    NotificationCompat
                        .Builder(context, EXPIRY_REMINDER_CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_corn_ms_emoji)
                        .setContentTitle(
                            when (daysBefore) {
                                0 -> "💣쿠폰 오늘 만료 에정"
                                1 -> "🚨내일 쿠폰 만료 예정"
                                7 -> "쿠폰 만료 일주일 전"
                                else -> "쿠폰 만료 ${daysBefore}일 전"
                            },
                        ).setContentText("${coupon.productName} 쿠폰이 곧 만료됩니다!")
                        .setGroup(EXPIRY_GROUP_KEY_COUPON)
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent)
                        .build()

                manager.notify(coupon.id.hashCode(), notification)
            }

            val summaryIntent = Intent(context, MainActivity::class.java)
            val summaryPendingIntent =
                PendingIntent.getActivity(
                    context,
                    0,
                    summaryIntent,
                    PendingIntent.FLAG_IMMUTABLE,
                )

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
                    .setContentIntent(summaryPendingIntent)
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
