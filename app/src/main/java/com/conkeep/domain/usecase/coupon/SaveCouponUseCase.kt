package com.conkeep.domain.usecase.coupon

import com.conkeep.data.local.file.LocalFileManager
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class SaveCouponUseCase
    @Inject
    constructor(
        private val fileManager: LocalFileManager,
    ) {
        /**
         * 쿠폰 이미지를 갤러리(Public Storage)로 내보냅니다.
         * @return 성공 여부 (null이면 실패)
         */
        suspend operator fun invoke(
            localPath: String?,
            name: String?,
            number: String?,
        ): Boolean {
            // 데이터 검증 (로직 중복 방지)
            if (localPath.isNullOrBlank()) return false

            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))

            // 파일명 조합
            val combinedInfo =
                when {
                    !name.isNullOrBlank() && !number.isNullOrBlank() -> "${name}_$number"
                    !name.isNullOrBlank() -> name
                    !number.isNullOrBlank() -> number
                    else -> "Unknown" // 둘 다 없을 경우
                }

            // 공백 제거 및 파일명 생성 (타임스탬프를 앞에 붙여 중복 방지)
            val sanitizedInfo = combinedInfo.replace("\\s".toRegex(), "_")
            val fileName = "ConKeep_${timestamp}_$sanitizedInfo"

            return fileManager.exportImageToPublic(localPath, fileName)
        }
    }
