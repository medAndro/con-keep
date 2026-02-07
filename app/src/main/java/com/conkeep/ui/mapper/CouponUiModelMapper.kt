package com.conkeep.ui.mapper

import com.conkeep.data.local.entity.CouponLocalStatus
import com.conkeep.domain.model.Coupon
import com.conkeep.ui.feature.coupon.model.CouponUiModel
import kotlinx.datetime.LocalDate

fun Coupon.toUiModel(today: LocalDate): CouponUiModel =
    CouponUiModel(
        id = id,
        number = couponPin ?: "",
        name = productName ?: "",
        expiryDate = expiryDate.toString(),
        isUsed = isUsed,
        isExpired = expiryDate?.let { it < today } ?: true,
        localImagePath = localImagePath,
        r2Url = imageUrl,
        isMonetary = isMonetary,
        amount = amount,
        localStatus =
            localStatus?.let { statusStr ->
                runCatching { CouponLocalStatus.valueOf(statusStr) }.getOrNull()
            },
    )

fun List<Coupon>.toUiModel(today: LocalDate): List<CouponUiModel> = map { it.toUiModel(today) }
