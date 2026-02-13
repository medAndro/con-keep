package com.conkeep.data.local.entity

enum class CouponStatus {
    /** 1. 초기 상태: 로컬 DB에만 생성됨 */
    PENDING,

    /** 2. 업로드 중: Cloudflare R2로 이미지 전송 중 (WorkManager 실행 중) */
    UPLOADING,

    /** 3. 분석 대기/진행 중: 서버(CF Worker)에 요청이 전달되어 AI가 작업 중 */
    ANALYZING,

    /** 4. 분석 완료: AI 파싱이 성공적으로 완료되어 데이터가 채워짐 */
    SUCCESS,

    /** 5. 분석 실패: AI가 텍스트를 읽지 못함 (사용자 수동 입력 유도) */
    AI_FAILED,

    /** 6. 업로드 실패: 네트워크 문제 등으로 R2 업로드 자체가 실패함 */
    UPLOAD_FAILED,
}
