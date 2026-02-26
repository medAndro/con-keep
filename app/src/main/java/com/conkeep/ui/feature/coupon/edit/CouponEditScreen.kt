package com.conkeep.ui.feature.coupon.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.conkeep.R
import com.conkeep.data.local.entity.CouponStatus
import com.conkeep.domain.model.ExpiryDate
import com.conkeep.navigation.Route
import com.conkeep.ui.component.EvenlyTextTopBar
import com.conkeep.ui.component.TopBarButtonConfig
import com.conkeep.ui.feature.coupon.edit.CouponEditViewModel
import com.conkeep.ui.feature.coupon.model.CouponUiModel
import com.conkeep.ui.theme.ConKeepColors.bgSurface
import com.conkeep.ui.theme.ConKeepColors.borderDefault
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CouponEditScreen(
    id: String,
    backStack: NavBackStack<NavKey>,
    viewModel: CouponEditViewModel,
) {
    val coupon by viewModel.coupon.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    CouponEditScreenContent(
        onBackClick = {
            if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                backStack.removeLastOrNull()
            }
        },
        onCouponSave = {},
        onImageClick = {
            coupon?.id?.takeIf { it.isNotEmpty() }?.let { couponId ->
                backStack.add(Route.CouponImageScreen(id = couponId))
            }
        },
        onUseCoupon = { viewModel.useCoupon() },
        coupon = coupon,
        id = id,
        modifier = Modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CouponEditScreenContent(
    onBackClick: () -> Unit,
    onCouponSave: () -> Unit,
    onImageClick: () -> Unit,
    onUseCoupon: () -> Unit,
    coupon: CouponUiModel?,
    id: String,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current

    val removeIcon: ImageVector = ImageVector.vectorResource(id = R.drawable.ic_close)

    Scaffold(
        topBar = {
            EvenlyTextTopBar(
                middleText = stringResource(R.string.coupon_edit_screen_title),
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
                            iconResId = R.drawable.ic_save,
                            contentDescription = stringResource(R.string.coupon_edit_screen_save_icon_description),
                            onClick = onCouponSave,
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
            Column(
                modifier =
                    modifier
                        .fillMaxWidth()
                        .background(color = bgSurface, shape = RoundedCornerShape(12.dp))
                        .padding(horizontal = 26.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.Top,
                ) {
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
                                ImageRequest
                                    .Builder(LocalContext.current)
                                    .data(coupon?.r2Url)
                                    .crossfade(true)
                                    .build(),
                            contentDescription = stringResource(R.string.coupon_card_image_description),
                            placeholder = painterResource(R.drawable.ic_corn_ms_emoji),
                            error = painterResource(R.drawable.ic_corn_ms_emoji),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize(),
                        )
                    }
                }
            }
        }
    }
}

private val fakeCoupon =
    CouponUiModel(
        id = "0",
        number = "1234-5678-9012",
        name = "스타벅스 아이스 아메리카노 T",
        brand = "스타벅스",
        expiryDate = ExpiryDate.Success(LocalDate.parse("2025-12-31")),
        isUsed = false,
        isExpired = false,
        status = CouponStatus.SUCCESS,
    )

@Preview(showBackground = true, name = "미사용 쿠폰")
@Composable
private fun CouponEditScreenContentPreview() {
    MaterialTheme {
        CouponEditScreenContent(
            onBackClick = {},
            onCouponSave = {},
            onImageClick = {},
            onUseCoupon = {},
            coupon = fakeCoupon,
            id = "0",
        )
    }
}
