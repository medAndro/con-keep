package com.conkeep.data.local.entity

enum class CouponLocalStatus {
    PREPROCESSED, // 전처리 완료 (분석중)
    RECOGNIZED, // AI 파싱 성공
    AI_FAILED, // AI 파싱 실패
    PENDING, // 초기/기본 상태
}
