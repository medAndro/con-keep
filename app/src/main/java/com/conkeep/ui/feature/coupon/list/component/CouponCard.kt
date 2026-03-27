package com.conkeep.ui.feature.coupon.list.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.conkeep.R
import com.conkeep.data.local.entity.CouponStatus
import com.conkeep.domain.model.ExpiryDate
import com.conkeep.ui.feature.coupon.model.CouponUiModel
import com.conkeep.ui.feature.coupon.model.badgeStatus
import com.conkeep.ui.theme.ConKeepColors.bgSurface
import com.conkeep.ui.theme.ConKeepColors.borderDefault
import com.conkeep.ui.theme.ConKeepColors.shimmerColor
import com.conkeep.ui.theme.ConKeepColors.textBrandGray
import com.conkeep.ui.theme.ConKeepTheme
import com.conkeep.ui.theme.PretendardMedium12
import com.conkeep.ui.theme.PretendardSemibold13
import com.conkeep.ui.theme.PretendardSemibold16
import com.valentinilk.shimmer.shimmer
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number

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
//            IconButton(
//                onClick = { /* 메뉴 열기 로직 */ },
//                modifier =
//                    Modifier
//                        .align(Alignment.TopEnd),
//            ) {
//                Icon(
//                    painter = painterResource(R.drawable.ic_more), // ... 아이콘
//                    contentDescription = "더보기 메뉴",
//                    tint = textPrimary,
//                )
//            }
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
                            ImageRequest
                                .Builder(LocalContext.current)
                                .data(couponUiModel.r2Url)
                                .crossfade(true)
                                .build(),
                        contentDescription = stringResource(R.string.coupon_card_image_description),
                        placeholder = painterResource(R.drawable.ic_launcher_foreground),
                        error = painterResource(R.drawable.ic_launcher_foreground),
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
                                        couponUiModel.amount.toIntOrNull() ?: 0,
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
                            couponUiModel.brand ?: "",
                            style = PretendardMedium12,
                            color = textBrandGray,
                        )
                    }
                    Text(
                        text =
                            when (couponUiModel.status) {
                                CouponStatus.ANALYZING -> "AI 인식중..."
                                CouponStatus.SUCCESS -> couponUiModel.name ?: ""
                                CouponStatus.AI_FAILED -> "AI 인식 실패..."
                                CouponStatus.PENDING -> "분석 대기중..."
                                CouponStatus.IMAGE_UPLOADING -> "이미지 업로드 중..."
                                CouponStatus.IMAGE_UPLOADED -> "이미지 업로드 완료"
                                CouponStatus.UPLOAD_FAILED -> "이미지 업로드 실패..."
                                CouponStatus.LOCAL_IMAGE_MISSING -> "서버에서 이미지 로딩중..."
                                CouponStatus.SERVER_IMAGE_MISSING -> "이미지를 찾을 수 없습니다"
                                CouponStatus.PERMANENT_FAILED -> "알 수 없는 문제로 업로드에 실패했습니다"
                                CouponStatus.DELETED -> "삭제된 쿠폰입니다"
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
                        when (couponUiModel.expiryDate) {
                            is ExpiryDate.Empty -> {
                            }

                            is ExpiryDate.Processing -> {
                            }

                            is ExpiryDate.Success ->
                                Text(
                                    text =
                                        stringResource(
                                            R.string.coupon_card_expiry_date_format,
                                            couponUiModel.expiryDate.value.year,
                                            couponUiModel.expiryDate.value.month.number,
                                            couponUiModel.expiryDate.value.day,
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

@Composable
fun ShimmerCouponCard(modifier: Modifier = Modifier) {
    Card(
        modifier =
            modifier
                .fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = bgSurface,
            ),
    ) {
        val roundShape = RoundedCornerShape(8.dp)

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier
                    .padding(13.dp)
                    .shimmer(),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(width = 84.dp, height = 87.dp)
                        .clip(roundShape)
                        .background(shimmerColor)
                        .border(1.dp, borderDefault, roundShape),
            )
            Column(
                modifier =
                    Modifier.padding(
                        start = 15.dp,
                        end = 3.dp,
                        top = 3.dp,
                        bottom = 3.dp,
                    ),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Row(
                    modifier = Modifier.height(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(80.dp, 12.dp)
                                .clip(roundShape)
                                .background(shimmerColor),
                    )
                }
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .size(0.dp, 16.dp)
                                .clip(roundShape)
                                .background(shimmerColor),
                    )
                }
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .size(0.dp, 16.dp)
                                .clip(roundShape)
                                .background(shimmerColor),
                    )
                }
                Row(
                    modifier =
                        Modifier
                            .height(21.dp)
                            .padding(bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(120.dp, 12.dp)
                                .clip(roundShape)
                                .background(shimmerColor),
                    )
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
                    expiryDate = ExpiryDate.Empty(),
                    dDay = 0,
                    isUsed = false,
                    isExpired = false,
                    localImagePath = null,
                    r2Url = null,
                    isMonetary = true,
                    amount = "1234567",
                    status = CouponStatus.SUCCESS,
                    isDirty = false,
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
                    expiryDate = ExpiryDate.Success(LocalDate.parse("2016-12-31")),
                    dDay = -10,
                    isUsed = false,
                    isExpired = false,
                    localImagePath = null,
                    r2Url = null,
                    isMonetary = false,
                    amount = "",
                    status = CouponStatus.SUCCESS,
                    isDirty = false,
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
                    expiryDate = ExpiryDate.Success(LocalDate.parse("2016-12-31")),
                    dDay = -123,
                    isUsed = true,
                    isExpired = false,
                    localImagePath = null,
                    r2Url = null,
                    isMonetary = false,
                    amount = "",
                    status = CouponStatus.SUCCESS,
                    isDirty = false,
                ),
            onClick = {},
        )
    }
}

@Preview
@Composable
fun CouponCardShimmerPreview() {
    ConKeepTheme {
        ShimmerCouponCard()
    }
}
