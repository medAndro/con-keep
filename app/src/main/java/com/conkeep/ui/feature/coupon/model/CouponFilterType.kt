package com.conkeep.ui.feature.coupon.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.conkeep.R

enum class CouponFilterType(
    val value: Int,
) {
    ALL(0),
    AVAILABLE(1),
    USED(2),
    EXPIRED(3),
}

@Composable
fun CouponFilterType.getDisplayTitle(): String =
    when (this) {
        CouponFilterType.ALL -> stringResource(R.string.filter_all)
        CouponFilterType.AVAILABLE -> stringResource(R.string.filter_available)
        CouponFilterType.USED -> stringResource(R.string.filter_used)
        CouponFilterType.EXPIRED -> stringResource(R.string.filter_expired)
    }
