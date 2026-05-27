package com.conkeep.notification

import com.conkeep.data.repository.coupon.CouponRepository
import com.conkeep.data.repository.coupon.ImminentCouponsResult
import com.conkeep.ui.feature.setting.notification.CouponAlarmSetting
import kotlinx.datetime.LocalTime
import javax.inject.Inject

class CouponAlarmHandler
    @Inject
    constructor(
        private val couponRepository: CouponRepository,
        private val notificationHelper: NotificationHelper,
        private val couponAlarmScheduler: CouponAlarmScheduler,
    ) {
        suspend fun handle(
            daysBefore: Int,
            targetTime: LocalTime,
        ) {
            when (val result = couponRepository.getImminentCouponsForAlarm(daysBefore)) {
                ImminentCouponsResult.AuthRequired -> {
                    notificationHelper.showAuthRequiredNotification()
                }

                is ImminentCouponsResult.Success -> {
                    if (result.coupons.isNotEmpty()) {
                        notificationHelper.showGroupedNotifications(daysBefore, result.coupons)
                    }
                }
            }

            couponAlarmScheduler.scheduleNextAlarm(
                CouponAlarmSetting(daysBefore, targetTime),
                true,
            )
        }
    }
