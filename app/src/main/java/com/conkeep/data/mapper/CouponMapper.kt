package com.conkeep.data.mapper

import com.conkeep.data.local.entity.CouponEntity
import com.conkeep.data.local.entity.CouponStatus
import com.conkeep.data.remote.dto.CouponDto
import com.conkeep.domain.model.Coupon
import com.conkeep.domain.model.ExpiryDate
import kotlinx.datetime.LocalDate
import java.time.OffsetDateTime
import kotlin.time.Instant

fun CouponEntity.toDomain(): Coupon =
    Coupon(
        id = id,
        userId = userId,
        imageUrl = imageUrl,
        productName = productName,
        brand = brand,
        couponPin = couponPin,
        expiryDate =
            runCatching {
                val date = expiryDate?.let { LocalDate.parse(it) }
                when {
                    date == null -> {
                        when (status) {
                            CouponStatus.PENDING.name -> ExpiryDate.Processing
                            CouponStatus.IMAGE_UPLOADING.name -> ExpiryDate.Processing
                            CouponStatus.ANALYZING.name -> ExpiryDate.Processing
                            else -> ExpiryDate.Empty()
                        }
                    }

                    else -> ExpiryDate.Success(date)
                }
            }.getOrElse {
                ExpiryDate.Empty()
            },
        isMonetary = isMonetary,
        amount = amount,
        category = category?.toCouponCategory(),
        userMemo = userMemo,
        isUsed = isUsed,
        // Long → LocalDateTime
        usedAt =
            usedAt?.let { epochMilli ->
                Instant
                    .fromEpochMilliseconds(epochMilli)
            },
        createdAt =
            Instant
                .fromEpochMilliseconds(createdAt),
        updatedAt =
            Instant
                .fromEpochMilliseconds(updatedAt),
        isSynced = isSynced,
        status = status,
        isDirty = isDirty,
    )

fun List<CouponEntity>.toDomain(): List<Coupon> = map { it.toDomain() }

fun Coupon.toEntity(): CouponEntity =
    CouponEntity(
        id = id,
        userId = userId,
        imageUrl = imageUrl,
        productName = productName,
        brand = brand,
        couponPin = couponPin,
        expiryDate = if (expiryDate is ExpiryDate.Success) expiryDate.value.toString() else null,
        isMonetary = isMonetary,
        amount = amount,
        category = category?.name,
        userMemo = userMemo,
        isUsed = isUsed,
        // kotlinx.Instant → Long (Unix timestamp)
        usedAt = usedAt?.toEpochMilliseconds(),
        createdAt = createdAt.toEpochMilliseconds(),
        updatedAt = updatedAt.toEpochMilliseconds(),
        isSynced = isSynced,
        status = status,
        isDirty = isDirty,
    )

fun List<Coupon>.toEntity(): List<CouponEntity> = map { it.toEntity() }

fun CouponDto.toEntity(): CouponEntity {
    // ISO 8601 문자열을 Long(Epoch Milli)으로 변환하는 헬퍼 함수
    fun String?.toEpochMilli(): Long =
        if (this.isNullOrBlank()) {
            0L
        } else {
            OffsetDateTime.parse(this).toInstant().toEpochMilli()
        }

    return CouponEntity(
        id = this.id,
        userId = this.userId,
        imageUrl = this.imageUrl,
        productName = this.productName ?: "",
        brand = this.brand ?: "",
        couponPin = this.couponPin ?: "",
        expiryDate = this.expiryDate,
        isMonetary = this.isMonetary,
        amount = this.amount,
        category = this.category,
        userMemo = this.userMemo ?: "",
        isUsed = this.isUsed,
        usedAt = this.usedAt.toEpochMilli().takeIf { it > 0 },
        createdAt = this.createdAt.toEpochMilli(),
        updatedAt = this.updatedAt.toEpochMilli(),
        isSynced = true, // 서버에서 받았으므로 동기화 완료
        status = this.status,
        isDirty = false, // 서버 데이터와 일치하므로 Dirty 해제
        isDeleted = this.isDeleted,
    )
}
