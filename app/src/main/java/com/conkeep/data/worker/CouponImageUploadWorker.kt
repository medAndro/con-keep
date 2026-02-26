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
import java.io.File

@HiltWorker
class CouponImageUploadWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted workerParams: WorkerParameters,
        private val couponRepository: CouponRepository,
    ) : CoroutineWorker(context, workerParams) {
        override suspend fun doWork(): Result {
            val couponId = inputData.getString("COUPON_ID") ?: return Result.failure()
            val localPath = inputData.getString("LOCAL_PATH") ?: return Result.failure()
            val mimeType = inputData.getString("MIME_TYPE") ?: "image/webp"
            val barcode = inputData.getString("BARCODE")
            val createdAt = inputData.getString("CREATED_AT") ?: ""

            return try {
                val file = File(localPath)
                if (!file.exists()) {
                    couponRepository.updateStatus(couponId, "FILE_LOST")
                    return Result.failure()
                }

                // 1. 프리사인드 URL 발급
                val urlResponse =
                    couponRepository.getPresignedUrl(file, couponId, mimeType).getOrThrow()

                // 2. R2 실제 업로드
                couponRepository.updateStatus(couponId, CouponStatus.UPLOADING.name)
                couponRepository
                    .uploadCouponImageR2(
                        file,
                        urlResponse.uploadPresignedUrl,
                        mimeType,
                    ).getOrThrow()

                couponRepository.updateR2Info(couponId, urlResponse.imageUrl)
                Log.d("CouponImageUploadWorker", "업로드 성공, 분석 시작")

                // 3. AI 분석 요청
                val aiResponse =
                    couponRepository.requestAiAnalyzeJob(
                        couponId,
                        urlResponse.imageUrl,
                        barcode,
                        createdAt,
                    )

                aiResponse.fold(
                    onSuccess = { message: String ->
                        Log.d("CouponImageUploadWorker", "AI 분석 요청 성공: $message")
                        couponRepository.updateStatus(couponId, CouponStatus.ANALYZING.name)
                        Result.success()
                    },
                    onFailure = { throwable ->
                        Log.e(
                            "CouponImageUploadWorker",
                            "AI 분석 요청 실패: ${throwable.message}",
                            throwable,
                        )
                        couponRepository.updateStatus(couponId, CouponStatus.UPLOAD_FAILED.name)
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
                Log.e("CouponImageUploadWorker", "네트워크 연결 오류: ${e.message}", e)
                handleNetworkError(couponId)
            } catch (e: Exception) {
                Log.e("CouponImageUploadWorker", "예상치 못한 오류: ${e.message}", e)
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
                "CouponImageUploadWorker",
                "state code: ${responseException.response.status.value} for coupon $couponId",
            )

            return when {
                responseException is ClientRequestException -> { // 클라이언트 오류
                    couponRepository.updateStatus(couponId, CouponStatus.UPLOAD_FAILED.name)
                    Result.failure()
                }

                responseException is ServerResponseException -> { // 서버 오류
                    if (runAttemptCount < 5) {
                        couponRepository.updateStatus(couponId, CouponStatus.UPLOADING.name)
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
            Log.d("CouponImageUploadWorker", "네트워크 연결 오류 재시도 $runAttemptCount/15")
            if (runAttemptCount < 15) {
                couponRepository.updateStatus(couponId, CouponStatus.UPLOADING.name)
                return Result.retry()
            } else {
                couponRepository.updateStatus(couponId, CouponStatus.AI_FAILED.name)
                return Result.failure()
            }
        }
    }
