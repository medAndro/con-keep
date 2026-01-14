package com.conkeep.ui.feature.coupon.list.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.conkeep.R
import com.conkeep.ui.feature.coupon.model.CouponFilterType
import com.conkeep.ui.feature.coupon.model.CouponSortType
import com.conkeep.ui.feature.coupon.model.getDisplayTitle
import com.conkeep.ui.theme.ConKeepColors.brandAccent
import com.conkeep.ui.theme.ConKeepColors.textPrimary
import com.conkeep.ui.theme.PretendardMedium16
import com.conkeep.ui.util.noRippleClickable

@Composable
fun CouponSortRow(
    totalCount: Int,
    selectFilterType: CouponFilterType,
    selectedSort: CouponSortType,
    isFilterExpanded: Boolean,
    onFilterChipExpandClick: () -> Unit,
    onSortClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sortTitle = selectFilterType.getDisplayTitle()
    val countText = totalCount.toString()
    val fullText = stringResource(id = R.string.coupon_count, sortTitle, totalCount)

    val annotatedString =
        buildAnnotatedString {
            val startIndex = fullText.indexOf(countText)
            if (startIndex != -1) {
                append(fullText)

                addStyle(
                    style =
                        SpanStyle(
                            color = brandAccent,
                        ),
                    start = startIndex,
                    end = startIndex + countText.length,
                )
            } else {
                append(fullText)
            }
        }

    Row(
        modifier =
            modifier
                .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier =
                Modifier
                    .noRippleClickable { onFilterChipExpandClick() }
                    .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = annotatedString,
                style = PretendardMedium16,
                color = textPrimary,
            )
            Icon(
                painter = painterResource(id = if (isFilterExpanded) R.drawable.ic_chevron_down else R.drawable.ic_chevron_up),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = textPrimary,
            )
        }

        Row(
            modifier = Modifier.noRippleClickable { onSortClick() },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = selectedSort.getDisplayTitle(),
                style = PretendardMedium16,
                color = textPrimary,
            )
            Icon(
                painter = painterResource(id = R.drawable.ic_switch_vertical),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = textPrimary,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CouponSortRowPreview() {
    CouponSortRow(
        totalCount = 10,
        selectFilterType = CouponFilterType.ALL,
        selectedSort = CouponSortType.RECENT,
        isFilterExpanded = false,
        onFilterChipExpandClick = {},
        onSortClick = {},
        modifier = Modifier,
    )
}

@Preview(showBackground = true)
@Composable
fun CouponSortRowPreview2() {
    CouponSortRow(
        totalCount = 30,
        selectFilterType = CouponFilterType.EXPIRED,
        selectedSort = CouponSortType.EXPIRY,
        isFilterExpanded = true,
        onFilterChipExpandClick = {},
        onSortClick = {},
        modifier = Modifier,
    )
}
