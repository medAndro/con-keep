package com.conkeep.ui.feature.coupon.list.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.conkeep.ui.feature.coupon.model.CouponCountSummary
import com.conkeep.ui.feature.coupon.model.CouponFilterType
import com.conkeep.ui.feature.coupon.model.getDisplayTitle
import com.conkeep.ui.theme.ConKeepColors.brandPrimary
import com.conkeep.ui.theme.ConKeepColors.brandSecondaryDarker
import com.conkeep.ui.theme.ConKeepColors.textPrimary
import com.conkeep.ui.theme.ConKeepColors.textSecondary
import com.conkeep.ui.theme.ConKeepTheme
import com.conkeep.ui.theme.PretendardSemibold13

@Composable
fun CouponFilterChipRow(
    selectedFilter: CouponFilterType,
    couponCountSummary: CouponCountSummary,
    onFilterSelected: (CouponFilterType) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(CouponFilterType.entries) { filter ->
            val isSelected = selectedFilter == filter

            FilterChip(
                selected = isSelected,
                onClick = { onFilterSelected(filter) },
                modifier =
                    Modifier
                        .height(27.dp),
                label = {
                    Text(
                        text = "${filter.getDisplayTitle()} · ${couponCountSummary.getCount(filter)}",
                        style = PretendardSemibold13,
                    )
                },
                shape = RoundedCornerShape(100.dp),
                colors =
                    FilterChipDefaults.filterChipColors(
                        selectedContainerColor = brandPrimary,
                        selectedLabelColor = textPrimary,
                        containerColor = brandSecondaryDarker,
                        labelColor = textSecondary,
                    ),
                border = null,
                leadingIcon = null,
                trailingIcon = null,
            )
        }
    }
}

val couponCountSummaryFixture =
    CouponCountSummary(
        counts =
            mapOf(
                CouponFilterType.ALL to 123,
                CouponFilterType.AVAILABLE to 22,
                CouponFilterType.USED to 10,
                CouponFilterType.EXPIRED to 42,
            ),
    )

@Preview
@Composable
fun CouponFilterChipRowALLPreview() {
    ConKeepTheme(darkTheme = false) {
        CouponFilterChipRow(
            selectedFilter = CouponFilterType.ALL,
            couponCountSummary = couponCountSummaryFixture,
            onFilterSelected = {},
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview
@Composable
fun CouponFilterChipRowUsedPreview() {
    ConKeepTheme(darkTheme = false) {
        CouponFilterChipRow(
            selectedFilter = CouponFilterType.USED,
            couponCountSummary = couponCountSummaryFixture,
            onFilterSelected = {},
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
