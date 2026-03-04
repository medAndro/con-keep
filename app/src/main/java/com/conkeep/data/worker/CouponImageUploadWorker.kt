package com.conkeep.data.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.conkeep.data.local.entity.CouponStatus
import com.conkeep.data.local.file.LocalFileManager
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
        private val localFileManager: LocalFileManager,
        private val couponRepository: CouponRepository,
    ) : CoroutineWorker(context, workerParams) {
        override suspend fun doWork(): Result {
            val couponId = inputData.getString("COUPON_ID") ?: return Result.failure()
            val localAbsolutePath =
                inputData.getString("LOCAL_ABSOLUTE_PATH_STRING") ?: return Result.failure()
            val isUploadImageOnly = inputData.getBoolean("UPLOAD_IMAGE_ONLY", false)

            return try {
                val file = File(localAbsolutePath)
                Log.d(TAG, "업로드할 이미지 파일: $file")
                if (!file.exists()) {
                    Log.e(TAG, "업로드할 이미지 파일이 존재하지 않습니다: $file")
                    couponRepository.updateStatus(couponId, "FILE_LOST")
                    return Result.failure()
                }
                val mimeType = localFileManager.getMimeTypeFromFile(file)
                Log.d(TAG, "업로드할 이미지 파일의 MIME 타입: $mimeType")

                // 1. 프리사인드 URL 발급
                val urlResponse =
                    couponRepository.getPresignedUrl(file, couponId, mimeType).getOrThrow()
                val finalImageUrl = urlResponse.imageUrl

                // 2. R2 실제 업로드
                couponRepository.updateStatus(couponId, CouponStatus.IMAGE_UPLOADING.name)

                val uploadResult =
                    couponRepository.uploadCouponImageR2(
                        imageFile = file,
                        uploadUrl = urlResponse.uploadPresignedUrl,
                        contentType = mimeType,
                    )

                return if (uploadResult.isSuccess) {
                    // 3. DB 업데이트
                    Log.d(TAG, "R2 업로드 성공: $finalImageUrl")
                    couponRepository.updateR2Info(couponId, finalImageUrl)
                    when (isUploadImageOnly) {
                        true -> couponRepository.updateStatus(couponId, CouponStatus.SUCCESS.name)
                        false ->
                            couponRepository.updateStatus(
                                couponId,
                                CouponStatus.IMAGE_UPLOADED.name,
                            )
                    }

                    // 4. 로컬 캐시 삭제 (성공했으므로)
                    if (file.exists()) file.delete()

                    // 5. WorkManager의 Result.success 반환
                    Result.success(workDataOf("IMAGE_URL" to finalImageUrl))
                } else {
                    // 업로드 실패 시 에러 추출
                    val error = uploadResult.exceptionOrNull()
                    Log.e(TAG, "R2 업로드 실패: ${error?.message}")

                    // 재시도 정책에 따라 분기
                    if (runAttemptCount < 3) {
                        Result.retry()
                    } else {
                        couponRepository.updateStatus(couponId, CouponStatus.UPLOAD_FAILED.name)
                        Result.failure()
                    }
                }
            } catch (e: ClientRequestException) {
                handleHttpError(couponId, e)
            } catch (e: ServerResponseException) {
                handleHttpError(couponId, e)
            } catch (e: IOException) {
                handleNetworkError(couponId, e)
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

            return when (responseException) {
                is ClientRequestException -> { // 클라이언트 오류
                    couponRepository.updateStatus(couponId, CouponStatus.UPLOAD_FAILED.name)
                    Result.failure()
                }

                is ServerResponseException -> { // 서버 오류
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

        private suspend fun handleNetworkError(
            couponId: String,
            responseException: IOException,
        ): Result {
            Log.d(TAG, "네트워크 연결 오류 재시도 $runAttemptCount/15, ${responseException.message}")
            if (runAttemptCount < 15) {
                couponRepository.updateStatus(couponId, CouponStatus.IMAGE_UPLOADING.name)
                return Result.retry()
            } else {
                couponRepository.updateStatus(couponId, CouponStatus.AI_FAILED.name)
                return Result.failure()
            }
        }

        companion object {
            private const val TAG = "CouponImageUploadWorker"
        }
    }
