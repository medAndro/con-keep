package com.conkeep.data.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.conkeep.data.local.entity.CouponStatus
import com.conkeep.data.repository.coupon.CouponRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.ServerResponseException
import kotlinx.io.IOException

@HiltWorker
class UpdateWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted workerParams: WorkerParameters,
        private val couponRepository: CouponRepository,
    ) : CoroutineWorker(context, workerParams) {
        override suspend fun doWork(): Result {
            val couponId = inputData.getString("COUPON_ID") ?: return Result.failure()

            return try {
                // 업데이트 요청
                val updateResponse =
                    couponRepository.updateJob(
                        couponId,
                    )

                updateResponse.fold(
                    onSuccess = { message: String ->
                        Log.d(TAG, "업데이트 요청 성공: $message")
                        Result.success()
                    },
                    onFailure = { throwable ->
                        Log.e(
                            TAG,
                            "업데이트 요청 실패: ${throwable.message}",
                            throwable,
                        )
                        Result.failure()
                    },
                )
            } catch (e: ClientRequestException) {
                // 4xx
                handleHttpError(couponId, e)
            } catch (e: ServerResponseException) {
                // 5xx
                handleHttpError(couponId, e)
            } catch (e: IOException) {
                // 네트워크 연결 오류
                Log.e(TAG, "네트워크 연결 오류: ${e.message}", e)
                handleNetworkError(couponId)
            } catch (e: Exception) {
                Log.e(TAG, "예상치 못한 오류: ${e.message}", e)
                if (runAttemptCount < 3) {
                    Result.retry()
                } else {
                    couponRepository.updateStatus(couponId, CouponStatus.PERMANENT_FAILED.name)
                    Result.failure()
                }
            }
        }

        private suspend fun handleHttpError(
            couponId: String,
            responseException: ResponseException,
        ): Result {
            Log.w(
                TAG,
                "state code: ${responseException.response.status.value} for coupon $couponId",
            )

            return when {
                responseException is ClientRequestException -> { // 클라이언트 오류
                    couponRepository.updateStatus(couponId, CouponStatus.UPLOAD_FAILED.name)
                    Result.failure()
                }

                responseException is ServerResponseException -> { // 서버 오류
                    if (runAttemptCount < 5) {
                        couponRepository.updateStatus(couponId, CouponStatus.IMAGE_UPLOADING.name)
                        Result.retry()
                    } else {
                        couponRepository.updateStatus(couponId, CouponStatus.AI_FAILED.name)
                        Result.failure()
                    }
                }

                else -> {
                    couponRepository.updateStatus(couponId, CouponStatus.UPLOAD_FAILED.name)
                    Result.failure()
                }
            }
        }

        private suspend fun handleNetworkError(couponId: String): Result {
            Log.d(TAG, "네트워크 연결 오류 재시도 $runAttemptCount/15")
            if (runAttemptCount < 15) {
                couponRepository.updateStatus(couponId, CouponStatus.IMAGE_UPLOADING.name)
                return Result.retry()
            } else {
                couponRepository.updateStatus(couponId, CouponStatus.AI_FAILED.name)
                return Result.failure()
            }
        }

        companion object {
            private const val TAG = "UpdateWorker"
        }
    }
