package com.conkeep.data.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.conkeep.data.repository.coupon.CouponRepository
import com.conkeep.notification.CouponAlarmScheduler
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.ServerResponseException
import kotlinx.io.IOException
import kotlin.coroutines.cancellation.CancellationException

@HiltWorker
class CouponSyncWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted workerParams: WorkerParameters,
        private val couponRepository: CouponRepository,
        private val couponAlarmScheduler: CouponAlarmScheduler,
    ) : CoroutineWorker(context, workerParams) {
        override suspend fun doWork(): Result =
            try {
                Log.d("Worker", "증분 동기화 시작")
                val syncResult = couponRepository.syncIncremental()
                couponAlarmScheduler.allAlarmRefresh()
                syncResult.fold(
                    onSuccess = {
                        Log.d("Worker", "증분 동기화 완료")
                        Result.success()
                    },
                    onFailure = {
                        Log.e("Worker", "증분 동기화 실패: ${it.message}")
                        Result.retry()
                    },
                )
            } catch (e: CancellationException) {
                Log.w("Worker", "증분 동기화 작업이 시스템에 의해 취소됨 (Doze 모드 등). 다시 예약됩니다.")
                throw e
            } catch (e: ClientRequestException) {
                // 4xx
                handleHttpError(e)
            } catch (e: ServerResponseException) {
                // 5xx
                handleHttpError(e)
            } catch (e: IOException) {
                // 네트워크 연결 오류
                Log.e("CouponSyncWorker", "네트워크 연결 오류: ${e.message}", e)
                handleNetworkError()
            } catch (e: Exception) {
                Log.e("CouponSyncWorker", "예상치 못한 오류: ${e.message}", e)
                if (runAttemptCount < 3) {
                    Result.retry()
                } else {
                    Result.failure()
                }
            }

        private fun handleHttpError(responseException: ResponseException): Result {
            Log.w(
                "CouponSyncWorker",
                "state code: ${responseException.response.status.value}",
            )

            return when {
                runAttemptCount < 5 -> {
                    Result.retry()
                }

                else -> {
                    Result.failure()
                }
            }
        }

        private fun handleNetworkError(): Result {
            Log.d("CouponImageUploadWorker", "네트워크 연결 오류 재시도 $runAttemptCount/15")
            return if (runAttemptCount < 15) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
