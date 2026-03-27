package com.conkeep.data.worker

import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CouponWorkManager
    @Inject
    constructor(
        private val workManager: WorkManager,
    ) {
        fun updateWorkerRequest(couponId: String): OneTimeWorkRequest {
            val uploadRequest =
                OneTimeWorkRequestBuilder<UpdateWorker>()
                    .setConstraints(
                        Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build(),
                    ).setInputData(
                        workDataOf(
                            "COUPON_ID" to couponId,
                        ),
                    ).build()
            return uploadRequest
        }

        fun enqueueDeleteRemoteCoupons(couponIds: List<String>) {
            val deleteRequest =
                OneTimeWorkRequestBuilder<DeleteWorker>()
                    .setConstraints(
                        Constraints
                            .Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build(),
                    ).setInputData(
                        workDataOf(
                            "COUPON_IDS" to couponIds.toTypedArray(),
                        ),
                    ).build()

            workManager.enqueueUniqueWork(
                "delete_batch_${System.currentTimeMillis()}",
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                deleteRequest,
            )
        }

        fun uploadImageWorkerRequest(
            couponId: String,
            localAbsolutePath: String,
        ): OneTimeWorkRequest {
            val uploadRequest =
                OneTimeWorkRequestBuilder<CouponImageUploadWorker>()
                    .setConstraints(
                        Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build(),
                    ).setInputData(
                        workDataOf(
                            "COUPON_ID" to couponId,
                            "LOCAL_ABSOLUTE_PATH_STRING" to localAbsolutePath,
                            "UPLOAD_IMAGE_ONLY" to true,
                        ),
                    ).build()
            return uploadRequest
        }

        fun enqueueWorkChain(
            workerName: String,
            requests: List<OneTimeWorkRequest>,
        ) {
            if (requests.isEmpty()) return

            // 1. 첫 번째 작업으로 시작 (WorkContinuation 객체 생성)
            var continuation =
                workManager.beginUniqueWork(
                    workerName,
                    ExistingWorkPolicy.REPLACE,
                    requests[0],
                )

            // 2. 두 번째 요소부터 반복문을 돌며 순차적으로 연결
            for (i in 1 until requests.size) {
                continuation = continuation.then(requests[i])
            }

            // 3. 최종적으로 큐에 삽입
            continuation.enqueue()
        }
    }
