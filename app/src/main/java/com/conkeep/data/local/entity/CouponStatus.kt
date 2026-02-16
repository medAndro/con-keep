package com.conkeep.data.local.entity

enum class CouponStatus {
    /** 초기 상태: MLKit으로 바코드가 분석된되어 로컬 DB에만 생성됨 */
    PENDING,

    /** 로컬에 이미지 없음: 쿠폰 이미지가 로컬에 없음, 웹에 존재하지는지는 모름, 존재한다면 다운로드 필요 */
    LOCAL_IMAGE_MISSING,

    /** 이미지 없음: 쿠폰 이미지가 로컬과 R2 어디에도 없음, 이미지 재업로드나 쿠폰 삭제 필요 */
    SERVER_IMAGE_MISSING,

    /** 업로드 중: Cloudflare R2로 이미지 전송 중 (WorkManager 실행 중) */
    UPLOADING,

    /** 분석 대기/진행 중: 서버(CF Worker)에 요청이 전달되어 AI가 작업 중 */
    ANALYZING,

    /** 분석 완료: AI 파싱이 성공적으로 완료되어 데이터가 채워짐 */
    SUCCESS,

    /** 분석 실패: AI가 텍스트를 분석을 실패함 (사용자 수동 입력 유도) */
    AI_FAILED,

    /** 업로드 실패: 네트워크 문제 등으로 R2 업로드 자체가 실패함 */
    UPLOAD_FAILED,

    /** 영구적 실패: 클라이언트 이슈로 업로드 자체가 실패함 */
    PERMANENT_FAILED,
}
