package com.conkeep.data.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.conkeep.data.remote.dto.CouponDto
import com.conkeep.data.repository.coupon.CouponRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
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

            // 1. 프리사인드 URL 발급
            val urlResponse =
                couponRepository.getPresignedUrl(file, couponId, mimeType).getOrThrow()

            // 2. R2 실제 업로드
            couponRepository
                .uploadCouponImageR2(
                    file,
                    urlResponse.uploadPresignedUrl,
                    mimeType,
                ).getOrThrow()

            // 3. AI 분석 요청 (직접 호출)
            val aiResponse =
                couponRepository.aiCouponRecognizing(
                    couponId,
                    urlResponse.imageUrl,
                    barcode,
                    createdAt,
                )

            aiResponse.fold(
                onSuccess = { couponDto: CouponDto ->
                    // AI 분석 성공 시 로컬 동기화
                    couponRepository.syncCouponFromServer(couponDto.copy(status = "SUCCESS"))
                    Result.success()
                },
                onFailure = {
                    // 서버는 살았는데 분석만 실패한 경우 (재시도 또는 실패 처리)
                    couponRepository.updateStatus(couponId, "AI_FAILED")
                    Result.failure()
                },
            )
        } catch (e: Exception) {
            Log.e("CouponImageUploadWorker", "업로드 중 오류: ${e.message}")
            // 네트워크 오류 등의 경우 재시도 (최대 횟수 내에서)
            if (runAttemptCount < 5) {
                Result.retry()
            } else {
                couponRepository.updateStatus(couponId, "AI_FAILED")
                Result.failure()
            }
        }
    }
}
