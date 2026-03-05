package com.conkeep.ui.feature.setting

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.conkeep.R
import com.conkeep.ui.theme.ConKeepColors.badgeWarning
import com.conkeep.ui.theme.ConKeepColors.badgeWarningBg
import com.conkeep.ui.theme.ConKeepColors.bgSurface
import com.conkeep.ui.theme.ConKeepColors.borderSubtle
import com.conkeep.ui.theme.ConKeepColors.brandSecondary
import com.conkeep.ui.theme.ConKeepColors.textPrimary
import com.conkeep.ui.theme.ConKeepColors.textSecondary
import com.conkeep.ui.theme.ConKeepTheme
import com.conkeep.ui.theme.PretendardMedium16
import com.conkeep.ui.theme.PretendardSemibold20
import kotlinx.datetime.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSetting(
    onAddNewAlarmClick: () -> Unit,
    modifier: Modifier = Modifier,
    couponAlarmSettings: List<CouponAlarmSetting> = emptyList(),
) {
    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            "알림 설정",
            style = PretendardSemibold20,
            color = textPrimary,
        )
        Column(
            modifier =
                modifier
                    .fillMaxWidth()
                    .background(color = bgSurface, shape = RoundedCornerShape(12.dp))
                    .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            when {
                couponAlarmSettings.isEmpty() -> {
                    Surface(
                        modifier =
                            modifier
                                .fillMaxWidth()
                                .height(36.dp),
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .wrapContentHeight(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "현재 알림을 받고 있지 않아요",
                                style = PretendardMedium16,
                                color = textSecondary,
                            )
                        }
                    }
                }

                else ->
                    couponAlarmSettings.forEach { couponAlarmSetting ->
                        NotificationItem(couponAlarmSetting = couponAlarmSetting, onTrashClick = {})
                    }
            }

            Surface(
                modifier =
                    modifier
                        .fillMaxWidth()
                        .height(36.dp),
                shape = RoundedCornerShape(12.dp),
                border =
                    BorderStroke(
                        0.6.dp,
                        borderSubtle,
                    ),
                onClick = onAddNewAlarmClick,
            ) {
                Box(
                    modifier =
                        Modifier
                            .wrapContentHeight(),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_plus_small),
                            contentDescription = "새 알림 추가",
                            tint = textSecondary,
                        )
                        Text(
                            text = "새 알림 추가",
                            style = PretendardMedium16,
                            color = textSecondary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationItem(
    couponAlarmSetting: CouponAlarmSetting,
    onTrashClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .height(36.dp),
        shape = RoundedCornerShape(12.dp),
        color = badgeWarningBg,
        border =
            BorderStroke(
                0.6.dp,
                borderSubtle,
            ),
    ) {
        Box(
            modifier =
                Modifier
                    .wrapContentHeight(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = couponAlarmSetting.getText(),
                style = PretendardMedium16,
                color = badgeWarning,
            )
            IconButton(
                onClick = onTrashClick,
                modifier =
                    Modifier
                        .size(36.dp)
                        .align(Alignment.TopEnd),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_trash),
                    contentDescription = "삭제",
                    tint = badgeWarning,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

data class CouponAlarmSetting(
    val daysBefore: Int,
    val targetTime: LocalTime,
) {
    fun getText(): String {
        // 날짜 변위 텍스트
        val dayText =
            when (daysBefore) {
                0 -> "당일"
                else -> "${daysBefore}일 전"
            }

        // 오전/오후 판정
        val amPm = if (targetTime.hour < 12) "오전" else "오후"

        // 12시간제 시간 계산
        val hour12 =
            when {
                targetTime.hour == 0 -> 12 // 00:00 -> 오전 12시
                targetTime.hour > 12 -> targetTime.hour - 12 // 13:00 -> 오후 1시
                else -> targetTime.hour // 12:00 -> 오후 12시 (위의 amPm에서 '오후' 처리됨)
            }

        return "만료 $dayText $amPm $hour12:00".trim()
    }
}

@Preview
@Composable
fun NotificationSettingEmptyPreview() {
    ConKeepTheme {
        Surface(color = brandSecondary) {
            NotificationSetting(
                onAddNewAlarmClick = {},
                couponAlarmSettings = emptyList(),
            )
        }
    }
}

@Preview
@Composable
fun NotificationSettingPreview() {
    ConKeepTheme {
        Surface(color = brandSecondary) {
            NotificationSetting(
                onAddNewAlarmClick = {},
                couponAlarmSettings =
                    listOf(
                        CouponAlarmSetting(0, LocalTime(12, 0)),
                        CouponAlarmSetting(1, LocalTime(9, 0)),
                    ),
            )
        }
    }
}
