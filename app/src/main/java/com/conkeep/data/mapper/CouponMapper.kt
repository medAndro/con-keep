package com.conkeep.data.mapper

import com.conkeep.data.local.entity.CouponEntity
import com.conkeep.data.local.entity.CouponLocalStatus
import com.conkeep.domain.model.Coupon
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
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
        expiryDate =
            expiryDate?.let { javaDate ->
                LocalDate(javaDate.year, javaDate.monthValue, javaDate.dayOfMonth)
            },
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
        localStatus = localStatus?.name, // 추가!
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
        // kotlinx.LocalDate → java.time.LocalDate
        expiryDate =
            expiryDate?.let { kDate ->
                java.time.LocalDate.of(kDate.year, kDate.month.number, kDate.day)
            },
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
        localStatus =
            localStatus?.let { statusStr ->
                runCatching { CouponLocalStatus.valueOf(statusStr) }.getOrNull()
            },
    )

fun List<Coupon>.toEntity(): List<CouponEntity> = map { it.toEntity() }
