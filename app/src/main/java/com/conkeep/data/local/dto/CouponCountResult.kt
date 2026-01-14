package com.conkeep.data.local.dto

/**
 * Room에서 쿼리 결과를 받기 위한 임시 DTO
 */
data class CouponCountResult(
    val filterValue: Int,
    val count: Int,
)
