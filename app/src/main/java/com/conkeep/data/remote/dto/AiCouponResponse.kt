package com.conkeep.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AiCouponResponse(
    val data: CouponInfo,
)

@Serializable
data class CouponInfo(
    @SerialName("product_name")
    val productName: String?,
    val brand: String?,
    @SerialName("coupon_pin")
    val couponPin: String?,
    @SerialName("expiry_date")
    val expiryDate: String?,
    @SerialName("is_monetary")
    val isMonetary: Boolean,
    val amount: Int? = null,
    val category: String,
)
