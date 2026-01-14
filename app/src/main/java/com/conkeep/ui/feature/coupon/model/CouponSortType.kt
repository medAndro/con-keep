package com.conkeep.ui.feature.coupon.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.conkeep.R

enum class CouponSortType(
    val value: Int,
) {
    RECENT(0),
    EXPIRY(1),
}

@Composable
fun CouponSortType.getDisplayTitle(): String =
    when (this) {
        CouponSortType.RECENT -> stringResource(R.string.sort_recent)
        CouponSortType.EXPIRY -> stringResource(R.string.sort_expiry)
    }
