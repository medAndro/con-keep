package com.conkeep.data.remote.dto

import androidx.room.TypeConverter
import com.conkeep.ui.feature.coupon.model.CouponSortType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CouponDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("image_url") val imageUrl: String?,
    @SerialName("product_name") val productName: String?,
    val brand: String?,
    @SerialName("coupon_pin") val couponPin: String?,
    @SerialName("expiry_date") val expiryDate: String?, // "2026-01-14"
    @SerialName("is_monetary") val isMonetary: Boolean,
    val amount: Int?,
    val category: String?,
    @SerialName("user_memo") val userMemo: String?,
    @SerialName("is_used") val isUsed: Boolean,
    @SerialName("used_at") val usedAt: String?, // ISO 8601 String
    @SerialName("created_at") val createdAt: String, // ISO 8601 String
    @SerialName("updated_at") val updatedAt: String, // ISO 8601 String
    val status: String,
    @SerialName("is_deleted") val isDeleted: Boolean,
)

class CouponTypeConverters {
    @TypeConverter
    fun fromSortType(type: CouponSortType): Int = type.sortType

    @TypeConverter
    fun toSortType(value: Int): CouponSortType = CouponSortType.entries.find { it.sortType == value } ?: CouponSortType.RECENT_ADD
}
