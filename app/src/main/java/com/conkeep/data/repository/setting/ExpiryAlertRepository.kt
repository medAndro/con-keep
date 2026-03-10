package com.conkeep.data.repository.setting

import com.conkeep.data.auth.SupabaseAuthManager
import com.conkeep.data.local.dao.ExpiryAlertDao
import com.conkeep.data.local.entity.ExpiryAlertEntity
import com.conkeep.data.mapper.toCouponAlarmSetting
import com.conkeep.data.mapper.toExpiryAlertEntity
import com.conkeep.ui.feature.setting.CouponAlarmSetting
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
class ExpiryAlertRepository
    @Inject
    constructor(
        private val expiryAlertDao: ExpiryAlertDao,
        private val authManager: SupabaseAuthManager,
    ) {
        fun getCouponAlarmSettings(): Flow<Set<CouponAlarmSetting>> =
            authManager.currentUserIdFlow
                .filterNotNull()
                .flatMapLatest { userId ->
                    expiryAlertDao
                        .getAlerts(
                            userId = userId,
                        ).map { expiryAlertEntities: List<ExpiryAlertEntity> ->
                            expiryAlertEntities.map { it.toCouponAlarmSetting() }.toSet()
                        }
                }

        suspend fun addCouponAlarmSetting(couponAlarmSetting: CouponAlarmSetting): Boolean {
            val userId = authManager.currentUserIdFlow.filterNotNull().first()
            val resultId = expiryAlertDao.insert(couponAlarmSetting.toExpiryAlertEntity(userId))

            // -1L이 반환되면 이미 존재해서 삽입되지 않음
            return resultId != -1L
        }

        suspend fun removeCouponAlarmSetting(couponAlarmSetting: CouponAlarmSetting): Boolean {
            val userId = authManager.currentUserIdFlow.filterNotNull().first()
            val entity = couponAlarmSetting.toExpiryAlertEntity(userId)
            val deletedCount = expiryAlertDao.delete(entity)
            return deletedCount == 1
        }
    }
