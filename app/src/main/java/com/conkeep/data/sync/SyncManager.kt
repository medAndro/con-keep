package com.conkeep.data.sync

import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.conkeep.data.worker.CouponSyncWorker
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncManager
    @Inject
    constructor(
        private val workManager: WorkManager,
    ) {
        /**
         * 쿠폰 증분 동기화 워커를 등록합니다.
         * 동일한 작업이 이미 실행 중이면 무시됩니다 (KEEP 정책).
         */
        fun enqueueCouponSync() {
            Log.d(TAG, "쿠폰 증분 동기화 워커 등록")

            val syncRequest =
                OneTimeWorkRequestBuilder<CouponSyncWorker>()
                    .setConstraints(
                        Constraints
                            .Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build(),
                    ).setBackoffCriteria(
                        BackoffPolicy.EXPONENTIAL,
                        30,
                        TimeUnit.SECONDS,
                    ).build()

            workManager.enqueueUniqueWork(
                SYNC_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                syncRequest,
            )

            Log.d(TAG, "워커 등록 완료: $SYNC_WORK_NAME")
        }

        /**
         * 쿠폰 동기화 워커를 취소합니다.
         */
        fun cancelCouponSync() {
            workManager.cancelUniqueWork(SYNC_WORK_NAME)
            Log.d(TAG, "워커 취소: $SYNC_WORK_NAME")
        }

        /**
         * 쿠폰 동기화 워커의 상태를 확인합니다.
         */
        fun getCouponSyncWorkInfo() = workManager.getWorkInfosForUniqueWorkLiveData(SYNC_WORK_NAME)

        companion object {
            private const val TAG = "SyncManager"
            private const val SYNC_WORK_NAME = "incremental_sync_coupon"
        }
    }
