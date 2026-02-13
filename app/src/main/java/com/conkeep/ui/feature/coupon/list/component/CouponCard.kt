package com.conkeep.ui.feature.coupon.list.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.conkeep.R
import com.conkeep.data.local.entity.CouponStatus
import com.conkeep.ui.feature.coupon.model.CouponUiModel
import com.conkeep.ui.feature.coupon.model.badgeStatus
import com.conkeep.ui.theme.ConKeepColors.bgSurface
import com.conkeep.ui.theme.ConKeepColors.borderDefault
import com.conkeep.ui.theme.ConKeepColors.textBrandGray
import com.conkeep.ui.theme.ConKeepColors.textPrimary
import com.conkeep.ui.theme.ConKeepTheme
import com.conkeep.ui.theme.PretendardMedium12
import com.conkeep.ui.theme.PretendardSemibold13
import com.conkeep.ui.theme.PretendardSemibold16
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number
import java.io.File

@Composable
fun CouponCard(
    couponUiModel: CouponUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cardAlpha = if (couponUiModel.isUsed || couponUiModel.isExpired) 0.8f else 1f
    val isPreview = LocalInspectionMode.current
    val imageShape = RoundedCornerShape(8.dp)

    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .alpha(cardAlpha)
                .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = bgSurface,
            ),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            IconButton(
                onClick = { /* 메뉴 열기 로직 */ },
                modifier =
                    Modifier
                        .align(Alignment.TopEnd),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_more), // ... 아이콘
                    contentDescription = "더보기 메뉴",
                    tint = textPrimary,
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(13.dp),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(width = 84.dp, height = 87.dp)
                            .clip(imageShape)
                            .border(1.dp, borderDefault, imageShape),
                ) {
                    AsyncImage(
                        model =
                            if (isPreview) {
                                R.drawable.ic_corn_ms_emoji
                            } else {
                                couponUiModel.localImagePath?.let {
                                    File(
                                        it,
                                    )
                                }
                            },
                        contentDescription = stringResource(R.string.coupon_card_image_description),
                        placeholder = painterResource(R.drawable.ic_corn_ms_emoji),
                        error = painterResource(R.drawable.ic_corn_ms_emoji),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize(),
                    )
                    if ((couponUiModel.isUsed || couponUiModel.isExpired) && couponUiModel.status == CouponStatus.SUCCESS) {
                        ExpirationBadge(
                            status = if (couponUiModel.isUsed) ExpirationBadgeStatus.Safe else ExpirationBadgeStatus.Expiring,
                            text =
                                if (couponUiModel.isUsed) {
                                    stringResource(
                                        R.string.filter_used,
                                    )
                                } else {
                                    stringResource(R.string.filter_expired)
                                },
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }
                    if (couponUiModel.isMonetary) {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(27.dp)
                                    .align(Alignment.BottomCenter)
                                    .background(borderDefault),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text =
                                    stringResource(
                                        R.string.coupon_card_monetary_value,
                                        couponUiModel.amount ?: 0,
                                    ),
                                style = PretendardSemibold13,
                                maxLines = 1,
                            )
                        }
                    }
                }
                Column(
                    modifier =
                        Modifier.padding(
                            start = 15.dp,
                            end = 0.dp,
                            top = 3.dp,
                            bottom = 3.dp,
                        ),
                ) {
                    Row(
                        modifier =
                            Modifier
                                .height(24.dp)
                                .padding(bottom = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            couponUiModel.brand,
                            style = PretendardMedium12,
                            color = textBrandGray,
                        )
                    }
                    Text(
                        text =
                            when (couponUiModel.status) {
                                CouponStatus.ANALYZING -> "AI 인식중..."
                                CouponStatus.SUCCESS -> couponUiModel.name
                                CouponStatus.AI_FAILED -> "AI 인식 실패..."
                                CouponStatus.PENDING -> "초기 상태"
                                CouponStatus.UPLOADING -> "업로드 중..."
                                CouponStatus.UPLOAD_FAILED -> "업로드 실패..."
                                null -> ""
                            },
                        style = PretendardSemibold16,
                        minLines = 2,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier =
                            Modifier
                                .padding(end = 13.dp)
                                .fillMaxWidth(),
                    )
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(21.dp)
                                .padding(top = 1.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (couponUiModel.expiryDate != null) {
                            Text(
                                text =
                                    stringResource(
                                        R.string.coupon_card_expiry_date_format,
                                        couponUiModel.expiryDate.year,
                                        couponUiModel.expiryDate.month.number,
                                        couponUiModel.expiryDate.day,
                                    ),
                                style = PretendardMedium12,
                                color = textBrandGray,
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        if (couponUiModel.dDay != null) {
                            ExpirationBadge(
                                status = couponUiModel.badgeStatus,
                                text =
                                    when {
                                        couponUiModel.dDay == 0 -> "D-0"
                                        couponUiModel.dDay > 0 -> "D+${couponUiModel.dDay}"
                                        else -> "D${couponUiModel.dDay}"
                                    },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun CouponCardMonetaryPreview() {
    ConKeepTheme {
        CouponCard(
            couponUiModel =
                CouponUiModel(
                    id = "1",
                    number = "1234567890",
                    name = "스타벅스 [간편한 한끼(HOT)] 카페 아메리카노T+탕종 파마산 치즈 베이글",
                    brand = "스타벅스",
                    expiryDate = LocalDate.parse("2026-12-31"),
                    dDay = 0,
                    isUsed = false,
                    isExpired = false,
                    localImagePath = null,
                    r2Url = null,
                    isMonetary = true,
                    amount = 1234567,
                    status = CouponStatus.SUCCESS,
                ),
            onClick = {},
        )
    }
}

@Preview
@Composable
fun CouponCardNormalPreview() {
    ConKeepTheme {
        CouponCard(
            couponUiModel =
                CouponUiModel(
                    id = "1",
                    number = "1234567890",
                    name = "스타벅스 [간편한 한끼(HOT)] 카페 아메리카노T+탕종 파마산 치즈 베이글",
                    brand = "스타벅스",
                    expiryDate = LocalDate.parse("2026-12-31"),
                    dDay = -10,
                    isUsed = false,
                    isExpired = false,
                    localImagePath = null,
                    r2Url = null,
                    isMonetary = false,
                    amount = null,
                    status = CouponStatus.SUCCESS,
                ),
            onClick = {},
        )
    }
}

@Preview
@Composable
fun CouponCardUsedPreview() {
    ConKeepTheme {
        CouponCard(
            couponUiModel =
                CouponUiModel(
                    id = "1",
                    number = "1234567890",
                    name = "스타벅스 [간편한 한끼(HOT)] 카페 아메리카노T+탕종 파마산 치즈 베이글+탕종 파마산 치즈 베이글+",
                    brand = "스타벅스",
                    expiryDate = LocalDate.parse("2026-12-31"),
                    dDay = -123,
                    isUsed = true,
                    isExpired = false,
                    localImagePath = null,
                    r2Url = null,
                    isMonetary = false,
                    amount = null,
                    status = CouponStatus.SUCCESS,
                ),
            onClick = {},
        )
    }
}
