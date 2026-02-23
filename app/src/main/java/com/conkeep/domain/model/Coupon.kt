package com.conkeep.domain.model

import kotlinx.datetime.LocalDate
import kotlin.time.Instant

data class Coupon(
    val id: String,
    val userId: String,
    // 이미지
    val imageUrl: String?,
    val localImagePath: String?,
    // 쿠폰 정보
    val productName: String?,
    val brand: String?,
    val couponPin: String?,
    val expiryDate: ExpiryDate,
    // 금액 정보
    val isMonetary: Boolean,
    val amount: Int?,
    // 분류 및 메모
    val category: CouponCategory?,
    val userMemo: String?,
    // 사용 정보
    val isUsed: Boolean,
    val usedAt: Instant?,
    // 메타데이터
    val createdAt: Instant,
    val updatedAt: Instant,
    val isSynced: Boolean,
    // 쿠폰 저장 상태
    val status: String,
)

sealed class ExpiryDate {
    data class Processing(
        val value: LocalDate = PROCESSING_DATE,
    ) : ExpiryDate() {
        override fun toString(): String = value.toString()
    }

    data class Success(
        val value: LocalDate,
    ) : ExpiryDate() {
        override fun toString(): String = value.toString()
    }

    data object Empty : ExpiryDate() {
        override fun toString(): String = ""
    }

    companion object {
        val PROCESSING_DATE = LocalDate(1980, 1, 1)
    }
}
