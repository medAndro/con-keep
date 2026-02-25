package com.conkeep.ui.feature.coupon.detail

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
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
import com.conkeep.ui.component.EvenlyTextTopBar
import com.conkeep.ui.component.RoundedDashedLine
import com.conkeep.ui.component.TopBarButtonConfig
import com.conkeep.ui.feature.coupon.common.rememberCouponActionHandler
import com.conkeep.ui.feature.coupon.component.AmountInputField
import com.conkeep.ui.feature.coupon.component.MemoInputField
import com.conkeep.ui.feature.coupon.list.component.ExpirationBadge
import com.conkeep.ui.feature.coupon.list.component.ExpirationBadgeStatus
import com.conkeep.ui.feature.coupon.model.CouponUiModel
import com.conkeep.ui.feature.coupon.model.badgeStatus
import com.conkeep.ui.theme.ConKeepColors.bgSurface
import com.conkeep.ui.theme.ConKeepColors.borderDefault
import com.conkeep.ui.theme.ConKeepColors.borderSubtle
import com.conkeep.ui.theme.ConKeepColors.buttonNegativeBg
import com.conkeep.ui.theme.ConKeepColors.buttonPositiveBg
import com.conkeep.ui.theme.ConKeepColors.textBrandGray
import com.conkeep.ui.theme.ConKeepColors.textPrimary
import com.conkeep.ui.theme.ConKeepColors.textSecondary
import com.conkeep.ui.theme.ConKeepColors.textWhite
import com.conkeep.ui.theme.PretendardBold18
import com.conkeep.ui.theme.PretendardMedium14
import com.conkeep.ui.theme.PretendardMedium16
import com.conkeep.ui.theme.PretendardSemibold14
import com.conkeep.ui.theme.PretendardSemibold16
import com.conkeep.ui.theme.PretendardSemibold24
import com.conkeep.ui.util.dpToPx
import com.conkeep.ui.util.toImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
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
    val context = LocalContext.current
    val memoSaveFailedMessage = stringResource(R.string.coupon_detail_screen_memo_save_failed)
    val amountSaveFailedMessage = stringResource(R.string.coupon_detail_screen_amount_save_failed)
    val actionHandler =
        rememberCouponActionHandler(
            onSaveRequested = { callback -> viewModel.saveCouponImage(callback) },
        )
    LaunchedEffect(Unit) {
        viewModel.errorEvent.collect { couponDetailError: CouponDetailError ->
            when (couponDetailError) {
                CouponDetailError.MemoSaveFailed -> {
                    Toast.makeText(context, memoSaveFailedMessage, Toast.LENGTH_SHORT).show()
                }

                CouponDetailError.AmountSaveFailed -> {
                    Toast.makeText(context, amountSaveFailedMessage, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

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
        onUseCoupon = viewModel::useCoupon,
        onUnUseCoupon = viewModel::unUseCoupon,
        onCouponImageSave = actionHandler.saveImage,
        onCouponNumberCopy = actionHandler.copyToClipboard,
        onCouponMemoSave = viewModel::saveCouponMemo,
        onCouponAmountSave = viewModel::saveCouponAmount,
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
    onUnUseCoupon: () -> Unit,
    onCouponImageSave: () -> Unit,
    onCouponNumberCopy: (String) -> Unit,
    onCouponMemoSave: (String) -> Unit,
    onCouponAmountSave: (Int) -> Unit,
    couponUiModel: CouponUiModel?,
    id: String,
    modifier: Modifier = Modifier,
    isPreview: Boolean = LocalInspectionMode.current,
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current

    val downloadImageVector: ImageVector = ImageVector.vectorResource(id = R.drawable.ic_download)
    val copyImageVector: ImageVector = ImageVector.vectorResource(id = R.drawable.ic_copy)

    val barcodeWidth = 220
    val barcodeHeight = 54
    val barcodeBitmap =
        remember(couponUiModel?.number) {
            if (couponUiModel?.number.isNullOrEmpty()) return@remember null

            try {
                val widthPx = barcodeWidth.dpToPx(context) * 2
                val heightPx = barcodeHeight.dpToPx(context) * 2
                val hints =
                    mutableMapOf<EncodeHintType, Any>().apply {
                        put(EncodeHintType.MARGIN, 0)
                        put(EncodeHintType.CHARACTER_SET, "UTF-8")
                    }

                val bitMatrix =
                    MultiFormatWriter().encode(
                        couponUiModel.number,
                        BarcodeFormat.CODE_128,
                        widthPx,
                        heightPx,
                        hints,
                    )

                bitMatrix.toImageBitmap()
            } catch (e: Exception) {
                Log.e("CouponDetailScreen", "바코드 생성 실패", e)
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
            EvenlyTextTopBar(
                middleText = stringResource(R.string.coupon_detail_screen_title),
                leftButtonConfigs =
                    listOf(
                        TopBarButtonConfig(
                            iconResId = R.drawable.ic_back,
                            contentDescription = stringResource(R.string.topbar_back_description),
                            onClick = onBackClick,
                        ),
                    ),
                rightButtonConfigs =
                    listOf(
                        TopBarButtonConfig(
                            iconResId = R.drawable.ic_edit,
                            contentDescription = stringResource(R.string.coupon_edit_screen_title),
                            onClick = onCouponEdit,
                        ),
                    ),
            )
        },
    ) { padding ->
        Column(
            modifier =
                modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding()
                    .verticalScroll(scrollState)
                    .padding(20.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = {
                            focusManager.clearFocus()
                        })
                    },
            verticalArrangement = Arrangement.spacedBy(18.dp),
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

                if (couponUiModel != null) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        Spacer(modifier = Modifier.width(44.dp))
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
                                        couponUiModel.localImagePath?.let { File(it) }
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
                        Spacer(modifier = Modifier.width(8.dp))

                        // 우측 다운로드 버튼
                        Surface(
                            onClick = onCouponImageSave,
                            color = Color.Transparent,
                            shape = CircleShape,
                            modifier = Modifier.size(36.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = downloadImageVector,
                                    contentDescription = "이미지 다운로드",
                                    tint = textPrimary,
                                    modifier = Modifier.size(24.dp),
                                )
                            }
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
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
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

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Spacer(Modifier.width(44.dp))
                                    Text(
                                        text = couponUiModel.number.chunked(4).joinToString(" "),
                                        style = PretendardSemibold16,
                                        textAlign = TextAlign.Center,
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    // 우측 복사 버튼
                                    Surface(
                                        onClick = {
                                            onCouponNumberCopy(couponUiModel.number)
                                        },
                                        color = Color.Transparent,
                                        shape = CircleShape,
                                        modifier = Modifier.size(36.dp),
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = copyImageVector,
                                                contentDescription = "바코드 복사",
                                                tint = textPrimary,
                                                modifier = Modifier.size(24.dp),
                                            )
                                        }
                                    }
                                }
                            }
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
            if (couponUiModel != null) {
                when {
                    !couponUiModel.isUsed -> {
                        Button(
                            onClick = onUseCoupon,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor = buttonPositiveBg,
                                    contentColor = textWhite,
                                ),
                        ) {
                            Text(
                                text = "사용 완료 처리",
                                style = PretendardBold18,
                            )
                        }
                    }

                    couponUiModel.isUsed -> {
                        Button(
                            onClick = onUnUseCoupon,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor = buttonNegativeBg,
                                    contentColor = textWhite,
                                ),
                        ) {
                            Text(
                                text = "사용 완료 취소",
                                style = PretendardBold18,
                            )
                        }
                    }
                }
            }

            if (couponUiModel?.isMonetary ?: false && couponUiModel.amount != null) {
                Column(
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        "잔액 관리 (금액권 수정)",
                        style = PretendardMedium16,
                        color = textSecondary,
                    )

                    // 잔액 관리 섹션
                    val currentCouponId = couponUiModel.id
                    val initialAmountText =
                        if (couponUiModel.amount == 0) "" else couponUiModel.amount.toString()

                    // ID가 바뀔 때만 typedAmount 초기화 (좀비 금액 방지)
                    var typedAmountText by rememberSaveable(currentCouponId) {
                        mutableStateOf(
                            initialAmountText,
                        )
                    }

                    //  Safety Net: 화면을 나갈 때 최종 상태를 저장
                    val latestCouponAmount by rememberUpdatedState(couponUiModel.amount.toString())
                    val latestAmountForExit by rememberUpdatedState(typedAmountText)
                    DisposableEffect(Unit) {
                        onDispose {
                            if ((latestCouponAmount.toIntOrNull() ?: 0)
                                != (latestAmountForExit.toIntOrNull() ?: 0)
                            ) {
                                Log.d("detail", "종료전 변경감지 저장실행됨 $latestAmountForExit")
                                onCouponAmountSave(
                                    latestAmountForExit
                                        .filter { it.isDigit() }
                                        .toIntOrNull() ?: 0,
                                )
                            }
                        }
                    }
                    AmountInputField(
                        amountText = typedAmountText,
                        onAmountChange = { typedAmountText = it },
                        onSave = { amount ->
                            onCouponAmountSave(amount)
                            Log.d("detail", "저장실행됨 $amount")
                        },
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    "메모",
                    style = PretendardMedium16,
                    color = textSecondary,
                )

                // 메모 섹션
                val currentCouponId = couponUiModel?.id ?: ""
                val initialMemo = couponUiModel?.memo ?: ""

                // ID가 바뀔 때만 typedMemo를 초기화 (좀비 메모 방지)
                var typedMemo by rememberSaveable(currentCouponId) { mutableStateOf(initialMemo) }

                // Safety Net: 화면을 나갈 때 최종 상태를 저장
                val latestCouponMemo by rememberUpdatedState(couponUiModel?.memo)
                val latestMemoForExit by rememberUpdatedState(typedMemo)
                DisposableEffect(Unit) {
                    onDispose {
                        if (latestCouponMemo != latestMemoForExit) {
                            Log.d("detail", "종료전 변경감지 저장실행됨 $latestMemoForExit")
                            onCouponMemoSave(latestMemoForExit)
                        }
                    }
                }
                MemoInputField(
                    memo = typedMemo,
                    onMemoChange = { typedMemo = it },
                    onSave = {
                        onCouponMemoSave(typedMemo)
                        Log.d("detail", "저장실행됨 $typedMemo")
                    },
                )
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
        memo = "생일 선물로 받은 쿠폰",
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
            onUnUseCoupon = {},
            onCouponImageSave = {},
            onCouponNumberCopy = {},
            onCouponMemoSave = {},
            onCouponAmountSave = {},
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
            onUnUseCoupon = {},
            onCouponImageSave = {},
            onCouponNumberCopy = {},
            onCouponMemoSave = {},
            onCouponAmountSave = {},
            couponUiModel =
                fakeCoupon.copy(
                    isExpired = true,
                    isUsed = true,
                    isMonetary = true,
                    amount = 3000,
                ),
            id = "0",
        )
    }
}

@Preview(showBackground = true, name = "쿠폰없음")
@Composable
private fun CouponDetailScreenNullContentPreview() {
    MaterialTheme {
        CouponDetailScreenContent(
            onBackClick = {},
            onCouponEdit = {},
            onImageClick = {},
            onUseCoupon = {},
            onUnUseCoupon = {},
            onCouponImageSave = {},
            onCouponNumberCopy = {},
            onCouponMemoSave = {},
            onCouponAmountSave = {},
            couponUiModel = null,
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
            onUnUseCoupon = {},
            onCouponImageSave = {},
            onCouponNumberCopy = {},
            onCouponMemoSave = {},
            onCouponAmountSave = {},
            couponUiModel = loadingCoupon,
            id = "0",
        )
    }
}
