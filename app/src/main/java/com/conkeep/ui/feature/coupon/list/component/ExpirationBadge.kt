package com.conkeep.ui.feature.coupon.list.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.conkeep.ui.theme.ConKeepColors.badgeCommon
import com.conkeep.ui.theme.ConKeepColors.badgeCommonBg
import com.conkeep.ui.theme.ConKeepColors.badgeExpiring
import com.conkeep.ui.theme.ConKeepColors.badgeExpiringBg
import com.conkeep.ui.theme.ConKeepColors.badgeSafe
import com.conkeep.ui.theme.ConKeepColors.badgeSafeBg
import com.conkeep.ui.theme.ConKeepColors.badgeWarning
import com.conkeep.ui.theme.ConKeepColors.badgeWarningBg
import com.conkeep.ui.theme.ConKeepTheme
import com.conkeep.ui.theme.PretendardSemibold12

sealed class ExpirationBadgeStatus {
    data object Expiring : ExpirationBadgeStatus()

    data object Warning : ExpirationBadgeStatus()

    data object Safe : ExpirationBadgeStatus()

    data object Common : ExpirationBadgeStatus()
}

@Composable
fun ExpirationBadge(
    status: ExpirationBadgeStatus,
    text: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = Modifier.height(21.dp),
        shape = RoundedCornerShape(8.dp),
        color =
            when (status) {
                ExpirationBadgeStatus.Expiring -> badgeExpiringBg
                ExpirationBadgeStatus.Warning -> badgeWarningBg
                ExpirationBadgeStatus.Safe -> badgeSafeBg
                ExpirationBadgeStatus.Common -> badgeCommonBg
            },
    ) {
        Box(
            modifier =
                Modifier
                    .padding(horizontal = 9.dp)
                    .wrapContentHeight(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                style = PretendardSemibold12,
                color =
                    when (status) {
                        ExpirationBadgeStatus.Expiring -> badgeExpiring
                        ExpirationBadgeStatus.Warning -> badgeWarning
                        ExpirationBadgeStatus.Safe -> badgeSafe
                        ExpirationBadgeStatus.Common -> badgeCommon
                    },
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
fun ExpirationBadgePreview() {
    ConKeepTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 임박 상태 (D-0, 오늘 만료 등)
            ExpirationBadge(
                status = ExpirationBadgeStatus.Expiring,
                text = "D-0",
            )

            // 경고 상태 (D-3, 유효기간 얼마 안 남음)
            ExpirationBadge(
                status = ExpirationBadgeStatus.Warning,
                text = "D-3",
            )

            // 안전 상태 (D-Day가 넉넉함)
            ExpirationBadge(
                status = ExpirationBadgeStatus.Safe,
                text = "D-30",
            )

            // 만료 상태 (D-Day가 지남)
            ExpirationBadge(
                status = ExpirationBadgeStatus.Common,
                text = "D+3",
            )

            // 사용완료 뱃지
            ExpirationBadge(
                status = ExpirationBadgeStatus.Safe,
                text = "사용완료",
            )
            // 기간만료 뱃지
            ExpirationBadge(
                status = ExpirationBadgeStatus.Warning,
                text = "기간만료",
            )
        }
    }
}
