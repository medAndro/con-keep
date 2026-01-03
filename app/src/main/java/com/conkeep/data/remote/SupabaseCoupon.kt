package com.conkeep.data.remote.dto

import com.conkeep.data.local.entity.CouponEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class SupabaseCoupon(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("image_url") val imageUrl: String,
    @SerialName("image_key") val imageKey: String,
    @SerialName("thumbnail_url") val thumbnailUrl: String? = null,
    @SerialName("product_name") val productName: String? = null,
    val brand: String? = null,
    @SerialName("coupon_pin") val couponPin: String? = null,
    @SerialName("expiry_date") val expiryDate: String? = null,
    @SerialName("is_monetary") val isMonetary: Boolean = false,
    val amount: Int? = null,
    val category: String? = null,
    @SerialName("user_memo") val userMemo: String? = null,
    @SerialName("is_used") val isUsed: Boolean = false,
    @SerialName("used_at") val usedAt: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
)

// DTO to Entity 변환
fun SupabaseCoupon.toEntity(): CouponEntity =
    CouponEntity(
        id = id,
        userId = userId,
        imageUrl = imageUrl,
        imageKey = imageKey,
        thumbnailUrl = thumbnailUrl,
        localImagePath = null,
        productName = productName,
        brand = brand,
        couponPin = couponPin,
        expiryDate = expiryDate,
        isMonetary = isMonetary,
        amount = amount,
        category = category,
        userMemo = userMemo,
        isUsed = isUsed,
        usedAt = usedAt?.let { Instant.parse(it).toEpochMilli() },
        createdAt = Instant.parse(createdAt).toEpochMilli(),
        updatedAt = Instant.parse(updatedAt).toEpochMilli(),
        isSynced = true,
    )

// Entity to DTO 변환
fun CouponEntity.toDto(): SupabaseCoupon =
    SupabaseCoupon(
        id = id,
        userId = userId,
        imageUrl = imageUrl,
        imageKey = imageKey,
        thumbnailUrl = thumbnailUrl,
        productName = productName,
        brand = brand,
        couponPin = couponPin,
        expiryDate = expiryDate,
        isMonetary = isMonetary,
        amount = amount,
        category = category,
        userMemo = userMemo,
        isUsed = isUsed,
        usedAt = usedAt?.let { Instant.ofEpochMilli(it).toString() },
        createdAt = Instant.ofEpochMilli(createdAt).toString(),
        updatedAt = Instant.ofEpochMilli(updatedAt).toString(),
    )
