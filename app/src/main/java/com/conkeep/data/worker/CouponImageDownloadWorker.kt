package com.conkeep.data.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.conkeep.data.repository.coupon.CouponRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class CouponImageDownloadWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted workerParams: WorkerParameters,
        private val couponRepository: CouponRepository,
    ) : CoroutineWorker(context, workerParams) {
        override suspend fun doWork(): Result {
            // ⭐ 디버그 로그 추가!
            Log.d(TAG, "=== Worker 시작 ===")
            Log.d(TAG, "inputData keys: ${inputData.keyValueMap.keys}")
            Log.d(TAG, "inputData values: ${inputData.keyValueMap}")

            val couponId = inputData.getString("COUPON_ID")
            val imageUrl = inputData.getString("IMAGE_URL")

            Log.d(TAG, "couponId: $couponId")
            Log.d(TAG, "imageUrl: $imageUrl")

            if (couponId == null) {
                Log.e(TAG, "COUPON_ID is null!")
                return Result.failure()
            }

            if (imageUrl == null) {
                Log.e(TAG, "IMAGE_URL is null!")
                return Result.failure()
            }

            return try {
                Log.d(TAG, "Repository 호출 시작: $couponId")
                val result = couponRepository.downloadAndSaveImage(couponId, imageUrl)

                result.fold(
                    onSuccess = { localPath ->
                        Log.d(TAG, "성공: $localPath")
                        Result.success()
                    },
                    onFailure = { exception ->
                        Log.e(TAG, "실패: ${exception.message}", exception)

                        if (runAttemptCount < MAX_RETRY_COUNT) {
                            Log.d(TAG, "재시도 $runAttemptCount/$MAX_RETRY_COUNT")
                            Result.retry()
                        } else {
                            Result.failure()
                        }
                    },
                )
            } catch (e: Exception) {
                Log.e(TAG, "예외 발생", e)
                Result.failure()
            }
        }

        companion object {
            private const val TAG = "CouponImageDownloadWorker"
            private const val MAX_RETRY_COUNT = 3
        }
    }
