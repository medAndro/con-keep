package com.conkeep.ui.feature.coupon.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.conkeep.R

enum class CouponSortType(
    val sortType: Int,
) {
    RECENT_ADD(0),
    EXPIRY(1),
    RECENT_USED(2),
}

@Composable
fun CouponSortType.getDisplayTitle(): String =
    when (this) {
        CouponSortType.RECENT_ADD -> stringResource(R.string.sort_recent)
        CouponSortType.EXPIRY -> stringResource(R.string.sort_expiry)
        CouponSortType.RECENT_USED -> stringResource(R.string.sort_recent_used)
    }
