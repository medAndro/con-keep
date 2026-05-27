package com.conkeep.notification

import com.conkeep.data.repository.coupon.CouponRepository
import com.conkeep.data.repository.coupon.ImminentCouponsResult
import com.conkeep.ui.feature.setting.notification.CouponAlarmSetting
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalTime
import org.junit.jupiter.api.Test
import org.robolectric.annotation.Config

@Config(manifest = Config.NONE)
class CouponAlarmHandlerTest {
    private val couponRepository = mockk<CouponRepository>()
    private val notificationHelper = mockk<NotificationHelper>()
    private val couponAlarmScheduler = mockk<CouponAlarmScheduler>()
    private val handler =
        CouponAlarmHandler(
            couponRepository = couponRepository,
            notificationHelper = notificationHelper,
            couponAlarmScheduler = couponAlarmScheduler,
        )

    @Test
    fun `인증 상태를 확인하지 못하면 안내 알림을 보낸다`() {
        val daysBefore = 1
        val targetTime = LocalTime(hour = 9, minute = 0)

        coEvery { couponRepository.getImminentCouponsForAlarm(daysBefore) } returns ImminentCouponsResult.AuthRequired
        every { notificationHelper.showAuthRequiredNotification() } just runs
        coEvery {
            couponAlarmScheduler.scheduleNextAlarm(
                CouponAlarmSetting(daysBefore, targetTime),
                true,
            )
        } returns true

        runTest {
            handler.handle(daysBefore, targetTime)
        }

        coVerify { couponRepository.getImminentCouponsForAlarm(daysBefore) }
        coVerify { couponAlarmScheduler.scheduleNextAlarm(CouponAlarmSetting(daysBefore, targetTime), true) }
        verify { notificationHelper.showAuthRequiredNotification() }
    }
}
