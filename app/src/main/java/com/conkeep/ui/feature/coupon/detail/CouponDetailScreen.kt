package com.conkeep.ui.feature.coupon.detail

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import coil3.compose.AsyncImage
import com.conkeep.R
import com.conkeep.data.local.entity.CouponStatus
import com.conkeep.domain.model.ExpiryDate
import com.conkeep.navigation.Route
import com.conkeep.ui.component.CenterRoundShimmer
import com.conkeep.ui.component.CenterRoundTextShimmer
import com.conkeep.ui.component.MiddleTextTopBar
import com.conkeep.ui.component.RoundedDashedLine
import com.conkeep.ui.component.TopBarButtonConfig
import com.conkeep.ui.feature.coupon.list.component.ExpirationBadge
import com.conkeep.ui.feature.coupon.list.component.ExpirationBadgeStatus
import com.conkeep.ui.feature.coupon.model.CouponUiModel
import com.conkeep.ui.feature.coupon.model.badgeStatus
import com.conkeep.ui.theme.ConKeepColors.bgSurface
import com.conkeep.ui.theme.ConKeepColors.borderDefault
import com.conkeep.ui.theme.ConKeepColors.borderSubtle
import com.conkeep.ui.theme.ConKeepColors.textBrandGray
import com.conkeep.ui.theme.PretendardMedium14
import com.conkeep.ui.theme.PretendardSemibold14
import com.conkeep.ui.theme.PretendardSemibold16
import com.conkeep.ui.theme.PretendardSemibold24
import com.conkeep.ui.util.dpToPx
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.valentinilk.shimmer.shimmer
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CouponDetailScreen(
    id: String,
    backStack: NavBackStack<NavKey>,
    viewModel: CouponDetailViewModel,
) {
    val coupon by viewModel.couponUiModel.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    CouponDetailScreenContent(
        onBackClick = {
            if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                backStack.removeLastOrNull()
            }
        },
        onCouponEdit = {
            coupon?.id?.takeIf { it.isNotEmpty() }?.let { couponId ->
                backStack.add(Route.CouponEditScreen(id = couponId))
            }
        },
        onImageClick = {
            coupon?.id?.takeIf { it.isNotEmpty() }?.let { couponId ->
                backStack.add(Route.CouponImageScreen(id = couponId))
            }
        },
        onUseCoupon = { viewModel.useCoupon() },
        couponUiModel = coupon,
        id = id,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CouponDetailScreenContent(
    onBackClick: () -> Unit,
    onCouponEdit: () -> Unit,
    onImageClick: () -> Unit,
    onUseCoupon: () -> Unit,
    couponUiModel: CouponUiModel?,
    id: String,
    modifier: Modifier = Modifier,
    isPreview: Boolean = LocalInspectionMode.current,
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val barcodeWidth = 220
    val barcodeHeight = 54
    val barcodeBitmap =
        remember(couponUiModel?.number) {
            try {
                val widthPx = barcodeWidth.dpToPx(context) * 2
                val heightPx = barcodeHeight.dpToPx(context) * 2
                val hints =
                    mutableMapOf<EncodeHintType, Any>().apply {
                        put(EncodeHintType.MARGIN, 0) // 기본 여백 제거
                        put(EncodeHintType.CHARACTER_SET, "UTF-8")
                    }

                val bitMatrix =
                    MultiFormatWriter().encode(
                        couponUiModel?.number,
                        BarcodeFormat.CODE_128,
                        widthPx,
                        heightPx,
                        hints,
                    )

                BarcodeEncoder().createBitmap(bitMatrix).asImageBitmap()
            } catch (e: Exception) {
                null
            }
        }
    val dDayText =
        remember(couponUiModel?.dDay) {
            when {
                couponUiModel?.dDay == null -> ""
                couponUiModel.dDay == 0 -> "D-0"
                couponUiModel.dDay > 0 -> "D+${couponUiModel.dDay}"
                else -> "D${couponUiModel.dDay}"
            }
        }
    Scaffold(
        topBar = {
            MiddleTextTopBar(
                middleText = stringResource(R.string.coupon_detail_screen_title),
                leftButtonConfig =
                    TopBarButtonConfig(
                        iconResId = R.drawable.ic_back,
                        contentDescription = stringResource(R.string.topbar_back_description),
                        onClick = onBackClick,
                    ),
                rightButtonConfig =
                    TopBarButtonConfig(
                        iconResId = R.drawable.ic_edit,
                        contentDescription = stringResource(R.string.coupon_edit_screen_title),
                        onClick = onCouponEdit,
                    ),
            )
        },
    ) { padding ->
        Column(
            modifier =
                modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(scrollState)
                    .padding(20.dp),
        ) {
            Log.d("CouponDetailScreen", "coupon: $couponUiModel")

            Column(
                modifier =
                    modifier
                        .fillMaxWidth()
                        .background(color = bgSurface, shape = RoundedCornerShape(12.dp))
                        .padding(horizontal = 26.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    when {
                        couponUiModel?.brand == null -> {
                            CenterRoundTextShimmer(
                                width = 90.dp,
                                context = context,
                                textSizeSp = 14f,
                            )
                        }

                        couponUiModel.brand == "" -> {}
                        else -> {
                            Text(
                                couponUiModel.brand,
                                style = PretendardMedium14,
                                color = textBrandGray,
                            )
                        }
                    }

                    when {
                        couponUiModel?.name == null -> {
                            CenterRoundTextShimmer(
                                width = 210.dp,
                                context = context,
                                textSizeSp = 16f,
                            )
                        }

                        couponUiModel.name == "" -> {}
                        else -> {
                            Text(
                                text = couponUiModel.name,
                                style = PretendardSemibold16,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }

                    if (couponUiModel?.brand == "" && couponUiModel.name == "") {
                        ExpirationBadge(
                            status = ExpirationBadgeStatus.Common,
                            text = "정보 없음",
                            textStyle = PretendardSemibold14,
                        )
                    }
                }

                when (couponUiModel) {
                    null -> {
                        Box(
                            modifier =
                                Modifier
                                    .size(width = 179.dp, height = 185.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, borderDefault, RoundedCornerShape(8.dp))
                                    .shimmer(),
                        )
                    }

                    else -> {
                        Box(
                            modifier =
                                Modifier
                                    .size(width = 179.dp, height = 185.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onImageClick() }
                                    .border(1.dp, borderDefault, RoundedCornerShape(8.dp)),
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
                                    textStyle = PretendardSemibold24,
                                    modifier =
                                        Modifier
                                            .align(Alignment.Center)
                                            .height(52.dp)
                                            .width(139.dp),
                                    shape = RoundedCornerShape(12.dp),
                                )
                            }
                        }

                        RoundedDashedLine(
                            color = borderSubtle,
                            strokeWidth = 2.dp,
                            dashLength = 6.dp,
                            dashGap = 4.dp,
                            modifier =
                                Modifier.padding(
                                    vertical = 0.dp,
                                    horizontal = 10.dp,
                                ),
                            // 상하 여백 조절
                        )
                        when {
                            couponUiModel.number == "" -> {
                                ExpirationBadge(
                                    status = ExpirationBadgeStatus.Common,
                                    text = "쿠폰번호 없음",
                                    textStyle = PretendardSemibold14,
                                )
                            }

                            couponUiModel.number != null -> {
                                if (barcodeBitmap != null) {
                                    Image(
                                        bitmap = barcodeBitmap,
                                        contentDescription = stringResource(R.string.coupon_detail_screen_barcode_image),
                                        modifier =
                                            Modifier
                                                .size(
                                                    width = barcodeWidth.dp,
                                                    height = barcodeHeight.dp,
                                                ),
                                        contentScale = ContentScale.FillWidth,
                                    )
                                } else {
                                    CenterRoundShimmer(
                                        width = barcodeWidth.dp,
                                        height = barcodeHeight.dp,
                                    )
                                }

                                Text(
                                    text = couponUiModel.number.chunked(4).joinToString(" "),
                                    style = PretendardSemibold16,
                                    textAlign = TextAlign.Center,
                                )
                            }

                            else -> {
                            }
                        }

                        when (couponUiModel.expiryDate) {
                            ExpiryDate.Empty -> {
                                ExpirationBadge(
                                    status = ExpirationBadgeStatus.Common,
                                    text = stringResource(R.string.coupon_detail_screen_empty_expiry_date),
                                    textStyle = PretendardSemibold14,
                                )
                            }

                            is ExpiryDate.Processing ->
                                CenterRoundTextShimmer(
                                    width = 190.dp,
                                    context = context,
                                    textSizeSp = 14f,
                                    verticalPaddingDp = 8.dp,
                                )

                            is ExpiryDate.Success -> {
                                ExpirationBadge(
                                    status = couponUiModel.badgeStatus,
                                    text =
                                        stringResource(
                                            R.string.coupon_detail_screen_expiry_date_with_d_day_format,
                                            couponUiModel.expiryDate.value.year,
                                            couponUiModel.expiryDate.value.month.number,
                                            couponUiModel.expiryDate.value.day,
                                            dDayText,
                                        ),
                                    textStyle = PretendardSemibold14,
                                )
                            }
                        }
                    }
                }
            }

            Text("쿠폰 ID: $id", style = MaterialTheme.typography.titleLarge)

            Spacer(modifier = Modifier.height(16.dp))

            couponUiModel.let {
                Text("번호: ${it?.number}")
                Text("이름: ${it?.name}")
                Text("유효기간: ${it?.expiryDate}")
                Text("상태: ${it?.status?.name}")
                Text("r2Url: ${it?.r2Url}")

                Spacer(modifier = Modifier.height(24.dp))

                if (it?.isUsed == false) {
                    Button(
                        onClick = onUseCoupon,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("쿠폰 사용하기")
                    }
                } else {
                    Text("이미 사용된 쿠폰입니다.")
                }
            }
        }
    }
}

private val fakeCoupon =
    CouponUiModel(
        id = "0",
        number = "1234-5678-9012",
        name = "부드러운 디저트 [카페 아메리카노 T 2잔 + 부드러운 생크림 카스텔라",
        brand = "스타벅스",
        expiryDate = ExpiryDate.Success(LocalDate.parse("2026-02-27")),
        isUsed = false,
        isExpired = false,
        dDay = -3,
        status = CouponStatus.SUCCESS,
    )

@Preview(showBackground = true, name = "미사용 쿠폰")
@Composable
private fun CouponDetailScreenContentPreview() {
    MaterialTheme {
        CouponDetailScreenContent(
            onBackClick = {},
            onCouponEdit = {},
            onImageClick = {},
            onUseCoupon = {},
            couponUiModel = fakeCoupon,
            id = "0",
        )
    }
}

@Preview(showBackground = true, name = "만료 쿠폰")
@Composable
private fun CouponDetailScreenExpiredContentPreview() {
    MaterialTheme {
        CouponDetailScreenContent(
            onBackClick = {},
            onCouponEdit = {},
            onImageClick = {},
            onUseCoupon = {},
            couponUiModel = fakeCoupon.copy(isExpired = true),
            id = "0",
        )
    }
}

private val loadingCoupon =
    CouponUiModel(
        id = "0",
        number = null,
        name = null,
        brand = null,
        expiryDate = ExpiryDate.Processing,
        isUsed = false,
        isExpired = false,
        dDay = -3,
        status = CouponStatus.SUCCESS,
    )

@Preview(showBackground = true, name = "인식중 쿠폰")
@Composable
private fun CouponDetailScreenLoadingContentPreview() {
    MaterialTheme {
        CouponDetailScreenContent(
            onBackClick = {},
            onCouponEdit = {},
            onImageClick = {},
            onUseCoupon = {},
            couponUiModel = loadingCoupon,
            id = "0",
        )
    }
}
