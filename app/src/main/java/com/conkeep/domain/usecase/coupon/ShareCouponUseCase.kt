// com.conkeep.domain.usecase.coupon.ShareCouponUseCase.kt
package com.conkeep.domain.usecase.coupon

import android.content.Context
import android.net.Uri
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import com.conkeep.data.local.file.LocalFileManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class ShareCouponUseCase
    @Inject
    constructor(
        private val fileManager: LocalFileManager,
        private val imageLoader: ImageLoader,
        @param:ApplicationContext private val context: Context,
    ) {
        /**
         * 쿠폰 이미지 URL을 기반으로 공유용 Uri를 생성합니다.
         * @return 성공 시 Uri, 실패 시 null
         */
        suspend operator fun invoke(
            r2Url: String?,
            name: String?,
            number: String?,
        ): Uri? {
            if (r2Url.isNullOrBlank()) return null

            // Coil을 통해 이미지 로드 및 캐시 확인
            val request =
                ImageRequest
                    .Builder(context)
                    .data(r2Url)
                    .build()

            val result = imageLoader.execute(request)
            if (result !is SuccessResult) return null

            // Coil 디스크 캐시에서 파일 경로 추출
            val diskCache = imageLoader.diskCache ?: return null
            val snapshot = diskCache.openSnapshot(r2Url) ?: return null

            return snapshot.use { snap: DiskCache.Snapshot ->
                val cacheFilePath = snap.data.toString()

                // 파일 이름 조합
                val combinedName =
                    when {
                        !name.isNullOrBlank() && !number.isNullOrBlank() -> "${name}_$number"
                        !name.isNullOrBlank() -> name
                        !number.isNullOrBlank() -> number
                        else -> "Conkeep_Coupon"
                    }

                // File Provider를 통해 공유용 Uri 생성
                fileManager.getShareUriWithCustomName(cacheFilePath, combinedName)
            }
        }
    }
