package com.conkeep.ui.mapper

import com.conkeep.data.local.entity.CouponLocalStatus
import com.conkeep.domain.model.Coupon
import com.conkeep.ui.feature.coupon.model.CouponUiModel

fun Coupon.toUiModel(): CouponUiModel =
    CouponUiModel(
        id = id,
        number = couponPin ?: "",
        name = productName ?: "",
        expiryDate = expiryDate.toString(),
        isUsed = isUsed,
        localImagePath = localImagePath,
        r2Url = imageUrl,
        localStatus =
            localStatus?.let { statusStr ->
                runCatching { CouponLocalStatus.valueOf(statusStr) }.getOrNull()
            },
    )

fun List<Coupon>.toUiModel(): List<CouponUiModel> = map { it.toUiModel() }
