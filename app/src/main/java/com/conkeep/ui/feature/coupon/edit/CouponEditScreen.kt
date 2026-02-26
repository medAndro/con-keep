package com.conkeep.ui.feature.coupon.detail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.conkeep.R
import com.conkeep.data.local.entity.CouponStatus
import com.conkeep.domain.model.ExpiryDate
import com.conkeep.navigation.Route
import com.conkeep.ui.component.EvenlyTextTopBar
import com.conkeep.ui.component.TopBarButtonConfig
import com.conkeep.ui.feature.coupon.edit.CouponEditViewModel
import com.conkeep.ui.feature.coupon.model.CouponUiModel
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
) {
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
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
        ) {
            Text("Hello CouponEdit!")

            Spacer(modifier = Modifier.height(16.dp))
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
