package com.conkeep.ui.feature.coupon.detail

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
import coil3.compose.AsyncImage
import com.conkeep.R
import com.conkeep.data.local.entity.CouponStatus
import com.conkeep.navigation.Route
import com.conkeep.ui.component.MiddleTextTopBar
import com.conkeep.ui.component.TopBarButtonConfig
import com.conkeep.ui.feature.coupon.model.CouponUiModel
import kotlinx.datetime.LocalDate
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CouponDetailScreen(
    id: String,
    backStack: NavBackStack<NavKey>,
    viewModel: CouponDetailViewModel,
) {
    val coupon by viewModel.coupon.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    CouponDetailScreenContent(
        onBackClick = {
            if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                backStack.removeLastOrNull()
            }
        },
        onCouponEdit = {},
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
private fun CouponDetailScreenContent(
    onBackClick: () -> Unit,
    onCouponEdit: () -> Unit,
    onImageClick: () -> Unit,
    onUseCoupon: () -> Unit,
    coupon: CouponUiModel?,
    id: String,
) {
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
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
        ) {
            Log.d("CouponDetailScreen", "coupon: $coupon")

            AsyncImage(
                model = File(coupon?.localImagePath ?: ""),
                contentDescription = "쿠폰 이미지",
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clickable(onClick = onImageClick),
            )

            Text("쿠폰 ID: $id", style = MaterialTheme.typography.titleLarge)

            Spacer(modifier = Modifier.height(16.dp))

            coupon.let {
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
        name = "스타벅스 아이스 아메리카노 T",
        brand = "스타벅스",
        expiryDate = LocalDate.parse("2025-12-31"),
        isUsed = false,
        isExpired = false,
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
            coupon = fakeCoupon,
            id = "0",
        )
    }
}
