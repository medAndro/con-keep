package com.conkeep.domain.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

data class Coupon(
    val id: String,
    val userId: String,
    // 이미지
    val imageUrl: String,
    val imageKey: String,
    val thumbnailUrl: String?,
    val localImagePath: String?,
    // 쿠폰 정보
    val productName: String?,
    val brand: String?,
    val couponPin: String?,
    val expiryDate: LocalDate?,
    // 금액 정보
    val isMonetary: Boolean,
    val amount: Int?,
    // 분류 및 메모
    val category: CouponCategory?,
    val userMemo: String?,
    // 사용 정보
    val isUsed: Boolean,
    val usedAt: LocalDateTime?,
    // 메타데이터
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val isSynced: Boolean,
)
