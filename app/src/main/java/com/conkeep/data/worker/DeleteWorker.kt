package com.conkeep.data.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.conkeep.data.repository.coupon.CouponRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import kotlinx.io.IOException

@HiltWorker
class DeleteWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted workerParams: WorkerParameters,
        private val couponRepository: CouponRepository,
    ) : CoroutineWorker(context, workerParams) {
        override suspend fun doWork(): Result {
            val couponIds = inputData.getStringArray("COUPON_IDS")?.toList()

            if (couponIds.isNullOrEmpty()) {
                Log.e(TAG, "삭제할 쿠폰 ID 리스트가 비어있습니다.")
                return Result.failure()
            }

            return try {
                val response = couponRepository.deleteCouponsRemote(couponIds)

                response.fold(
                    onSuccess = { result ->
                        Log.d(TAG, "삭제 성공: ${result.deletedCount}개 삭제됨")
                        Result.success()
                    },
                    onFailure = { throwable ->
                        handleError(throwable)
                    },
                )
            } catch (e: Exception) {
                handleError(e)
            }
        }

        private fun handleError(throwable: Throwable): Result {
            Log.e(TAG, "삭제 작업 중 오류 발생: ${throwable.message}")

            return when (throwable) {
                is ClientRequestException -> {
                    Result.failure()
                }

                is ServerResponseException, is IOException -> {
                    if (runAttemptCount < 3) {
                        Result.retry()
                    } else {
                        Result.failure()
                    }
                }

                else -> Result.failure()
            }
        }

        companion object {
            private const val TAG = "DeleteWorker"
        }
    }
