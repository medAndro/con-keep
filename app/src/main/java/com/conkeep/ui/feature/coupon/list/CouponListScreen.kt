package com.conkeep.ui.feature.coupon.list

import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.conkeep.data.local.entity.CouponLocalStatus
import com.conkeep.navigation.Route
import com.conkeep.ui.feature.coupon.list.component.CouponCard
import com.conkeep.ui.feature.coupon.list.component.SearchBar
import com.conkeep.ui.feature.coupon.model.CouponUiModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CouponScreen(
    backStack: NavBackStack<NavKey>,
    viewModel: CouponListViewModel = hiltViewModel(),
) {
    val coupons: List<CouponUiModel> by viewModel.coupons.collectAsState()

    val pickMedia: ManagedActivityResultLauncher<PickVisualMediaRequest, Uri?> =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia(),
        ) { uri ->
            uri?.let {
                viewModel.addCouponFromUri(uri)
            }
        }

    CouponScreenContent(
        onCouponAddClick = {
            pickMedia.launch(
                PickVisualMediaRequest(
                    ActivityResultContracts.PickVisualMedia.ImageOnly,
                ),
            )
        },
        onCouponDetailClick = { couponId ->
            backStack.add(Route.CouponDetailScreen(id = couponId))
        },
        coupons = coupons,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CouponScreenContent(
    coupons: List<CouponUiModel> = emptyList(),
    onCouponAddClick: () -> Unit,
    onCouponDetailClick: (String) -> Unit,
    searchQuery: String = "",
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("내 쿠폰") },
                actions = {
                    Button(onClick = onCouponAddClick) {
                        Text("+")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .padding(padding) // Scaffold의 패딩(TopAppBar 높이 등)을 여기에 적용
                    .fillMaxSize(),
        ) {
            SearchBar(
                query = searchQuery,
                onQueryUpdate = {},
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )

            LazyColumn(
                modifier = Modifier.weight(1f), // 남은 영역을 리스트가 차지하도록 설정
            ) {
                items(coupons) { coupon ->
                    CouponCard(
                        couponUiModel = coupon,
                        onClick = { onCouponDetailClick(coupon.id) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
}

private val dummyCoupons =
    listOf(
        CouponUiModel(
            id = "1",
            number = "1234-5678-9012",
            name = "스타벅스 아이스 아메리카노 T",
            expiryDate = "2025.12.31",
            isUsed = false,
            localStatus = CouponLocalStatus.RECOGNIZED,
        ),
        CouponUiModel(
            id = "2",
            number = "9876-5432-1098",
            name = "배스킨라빈스 싱글레귤러",
            expiryDate = "2025.12.15",
            isUsed = false,
            localStatus = CouponLocalStatus.PREPROCESSED,
        ),
        CouponUiModel(
            id = "3",
            number = "1111-2222-3333",
            name = "[기프티콘] 파리바게뜨 우유식빵",
            expiryDate = "2026.01.20",
            isUsed = true,
            localStatus = CouponLocalStatus.AI_FAILED,
        ),
        CouponUiModel(
            id = "4",
            number = "5555-6666-7777",
            name = "교촌치킨 허니콤보 웨지감자 세트",
            expiryDate = "2026.02.10",
            isUsed = false,
            localStatus = CouponLocalStatus.PENDING,
        ),
    )

@Preview(showBackground = true)
@Composable
private fun CouponScreenContentPreview() {
    CouponScreenContent(
        coupons = dummyCoupons,
        onCouponAddClick = { },
        onCouponDetailClick = {},
    )
}
