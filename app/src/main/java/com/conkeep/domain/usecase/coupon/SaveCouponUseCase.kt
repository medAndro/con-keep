package com.conkeep.domain.usecase.coupon

import com.conkeep.data.local.file.LocalFileManager
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
            // 1. 데이터 검증 (로직 중복 방지)
            if (localPath.isNullOrBlank()) return false

            // 2. 파일명 규칙 정의 (중요: 여기서 규칙을 정의하면 모든 화면에서 동일한 파일명 사용 가능!)
            val fileName = "${name.orEmpty().replace(" ", "_")}_${number.orEmpty()}"

            // 3. 파일 매니저 실행
            return fileManager.exportImageToPublic(localPath, fileName)
        }
    }
