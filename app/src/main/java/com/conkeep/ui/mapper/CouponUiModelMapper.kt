package com.conkeep.ui.mapper

import com.conkeep.data.local.entity.CouponStatus
import com.conkeep.domain.model.Coupon
import com.conkeep.domain.model.ExpiryDate
import com.conkeep.ui.feature.coupon.model.CouponUiModel
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil

fun Coupon.toUiModel(today: LocalDate): CouponUiModel {
    val expiryLocalDate: LocalDate? =
        if (expiryDate is ExpiryDate.Success) {
            expiryDate.value
        } else {
            null
        }
    val dDayValue = expiryLocalDate?.daysUntil(today)
    val status =
        runCatching { CouponStatus.valueOf(status) }.getOrElse { CouponStatus.PERMANENT_FAILED }

    return CouponUiModel(
        id = id,
        number = couponPin,
        name = productName,
        brand = brand,
        expiryDate = expiryDate,
        dDay = dDayValue,
        isUsed = isUsed,
        isExpired = expiryLocalDate?.let { it < today } ?: true,
        localImagePath = localImagePath,
        r2Url = imageUrl,
        isMonetary = isMonetary,
        amount = amount,
        status = status,
        memo = userMemo ?: "",
    )
}

fun List<Coupon>.toUiModel(today: LocalDate): List<CouponUiModel> = map { it.toUiModel(today) }
