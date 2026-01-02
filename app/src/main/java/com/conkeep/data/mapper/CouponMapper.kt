// data/mapper/CouponMapper.kt
package com.conkeep.data.mapper

import com.conkeep.data.local.entity.CouponEntity
import com.conkeep.domain.model.Coupon
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

fun CouponEntity.toDomain(): Coupon =
    Coupon(
        id = id,
        userId = userId,
        imageUrl = imageUrl,
        imageKey = imageKey,
        thumbnailUrl = thumbnailUrl,
        productName = productName,
        brand = brand,
        couponPin = couponPin,
        expiryDate = expiryDate?.let { LocalDate.parse(it) },
        isMonetary = isMonetary,
        amount = amount,
        category = category?.toCouponCategory(),
        userMemo = userMemo,
        isUsed = isUsed,
        usedAt =
            usedAt?.let {
                Instant
                    .fromEpochMilliseconds(it)
                    .toLocalDateTime(TimeZone.currentSystemDefault())
            },
        createdAt =
            Instant
                .fromEpochMilliseconds(createdAt)
                .toLocalDateTime(TimeZone.currentSystemDefault()),
        updatedAt =
            Instant
                .fromEpochMilliseconds(updatedAt)
                .toLocalDateTime(TimeZone.currentSystemDefault()),
    )

fun List<CouponEntity>.toDomain(): List<Coupon> = map { it.toDomain() }

fun Coupon.toEntity(): CouponEntity =
    CouponEntity(
        id = id,
        userId = userId,
        imageUrl = imageUrl,
        imageKey = imageKey,
        thumbnailUrl = thumbnailUrl,
        productName = productName,
        brand = brand,
        couponPin = couponPin,
        expiryDate = expiryDate?.toString(), // "2026-02-01"
        isMonetary = isMonetary,
        amount = amount,
        category = category?.name, // Enum → "CAFE"
        userMemo = userMemo,
        isUsed = isUsed,
        usedAt = usedAt?.toInstant(TimeZone.currentSystemDefault())?.toEpochMilliseconds(),
        createdAt = createdAt.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds(),
        updatedAt = updatedAt.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds(),
    )

fun List<Coupon>.toEntity(): List<CouponEntity> = map { it.toEntity() }
