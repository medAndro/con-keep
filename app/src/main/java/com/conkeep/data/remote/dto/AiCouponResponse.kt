package com.conkeep.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AiCouponResponse(
    val success: Boolean,
    val data: CouponDto,
    @SerialName("db_status") val dbStatus: String,
    val metadata: AiMetadata? = null,
)

@Serializable
data class AiMetadata(
    val userId: String,
    val couponId: String,
)
