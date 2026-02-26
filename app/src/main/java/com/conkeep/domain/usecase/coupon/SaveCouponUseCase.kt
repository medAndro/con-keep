package com.conkeep.domain.usecase.coupon

import android.content.Context
import android.util.Log
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import com.conkeep.data.local.file.LocalFileManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class SaveCouponUseCase
    @Inject
    constructor(
        private val fileManager: LocalFileManager,
        private val imageLoader: ImageLoader,
        @param:ApplicationContext private val context: Context,
    ) {
        /**
         * 쿠폰 이미지를 갤러리(Public Storage)로 내보냅니다.
         * @return 성공 여부 (null이면 실패)
         */
        suspend operator fun invoke(
            r2Url: String?,
            name: String?,
            number: String?,
        ): Boolean {
            // 데이터 검증 (로직 중복 방지)
            if (r2Url.isNullOrBlank()) return false

            // Coil을 통해 이미지 로드 (캐시에 있으면 즉시 반환, 없으면 다운로드 후 캐싱)
            val request =
                ImageRequest
                    .Builder(context)
                    .data(r2Url)
                    .build()

            val result = imageLoader.execute(request)
            if (result !is SuccessResult) return false

            // Coil 디스크 캐시에서 해당 이미지의 물리적 파일 경로를 가져옵니다.
            val diskCache = imageLoader.diskCache ?: return false
            val snapshot = diskCache.openSnapshot(r2Url) ?: return false

            return snapshot.use { snapshot: DiskCache.Snapshot ->
                // 캐시 파일의 경로 (okio.Path를 String으로 변환)
                val cacheFilePath = snapshot.data.toString()

                val timestamp =
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                val combinedInfo =
                    when {
                        !name.isNullOrBlank() && !number.isNullOrBlank() -> "${name}_$number"
                        !name.isNullOrBlank() -> name
                        !number.isNullOrBlank() -> number
                        else -> "Unknown"
                    }

                val sanitizedInfo = combinedInfo.replace("\\s".toRegex(), "_")
                val fileName = "ConKeep_${timestamp}_$sanitizedInfo"

                Log.d("SaveCouponUseCase", "cacheFilePath: $cacheFilePath")
                // 캐시된 파일을 Public Storage로 복사
                fileManager.exportImageToPublic(cacheFilePath, fileName)
            }
        }
    }
