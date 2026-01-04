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
        remoteId = remoteId,
        userId = userId,
        imageUrl = imageUrl,
        imageKey = imageKey,
        thumbnailUrl = thumbnailUrl,
        localImagePath = localImagePath,
        productName = productName,
        brand = brand,
        couponPin = couponPin,
        expiryDate = expiryDate?.let { LocalDate.parse(it) },
        isMonetary = isMonetary,
        amount = amount,
        category = category?.toCouponCategory(),
        userMemo = userMemo,
        isUsed = isUsed,
        // Long → LocalDateTime
        usedAt =
            usedAt?.let { epochMilli ->
                kotlinx.datetime.Instant
                    .fromEpochMilliseconds(epochMilli)
                    .toLocalDateTime(TimeZone.currentSystemDefault())
            },
        createdAt =
            kotlinx.datetime.Instant
                .fromEpochMilliseconds(createdAt)
                .toLocalDateTime(TimeZone.currentSystemDefault()),
        updatedAt =
            kotlinx.datetime.Instant
                .fromEpochMilliseconds(updatedAt)
                .toLocalDateTime(TimeZone.currentSystemDefault()),
        isSynced = isSynced,
        localStatus = localStatus, // 추가!
    )

fun List<CouponEntity>.toDomain(): List<Coupon> = map { it.toDomain() }

fun Coupon.toEntity(): CouponEntity =
    CouponEntity(
        id = id,
        remoteId = remoteId,
        userId = userId,
        imageUrl = imageUrl,
        imageKey = imageKey,
        thumbnailUrl = thumbnailUrl,
        localImagePath = localImagePath,
        productName = productName,
        brand = brand,
        couponPin = couponPin,
        expiryDate = expiryDate?.toString(),
        isMonetary = isMonetary,
        amount = amount,
        category = category?.name,
        userMemo = userMemo,
        isUsed = isUsed,
        // kotlinx.LocalDateTime → Long (Unix timestamp)
        usedAt = usedAt?.toInstant(TimeZone.currentSystemDefault())?.toEpochMilliseconds(),
        createdAt = createdAt.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds(),
        updatedAt = updatedAt.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds(),
        isSynced = isSynced,
        localStatus = localStatus,
    )

fun List<Coupon>.toEntity(): List<CouponEntity> = map { it.toEntity() }
