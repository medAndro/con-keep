package com.conkeep.ui.feature.coupon.list

import android.net.Uri
import android.util.Log
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.conkeep.data.local.entity.CouponLocalStatus
import com.conkeep.navigation.Route
import com.conkeep.ui.component.TopBar
import com.conkeep.ui.feature.coupon.list.component.CouponCard
import com.conkeep.ui.feature.coupon.list.component.CouponFilterChipRow
import com.conkeep.ui.feature.coupon.list.component.CouponSortRow
import com.conkeep.ui.feature.coupon.list.component.SearchBar
import com.conkeep.ui.feature.coupon.list.component.couponCountSummaryFixture
import com.conkeep.ui.feature.coupon.model.CouponCountHeaderState
import com.conkeep.ui.feature.coupon.model.CouponCountSummary
import com.conkeep.ui.feature.coupon.model.CouponFilterType
import com.conkeep.ui.feature.coupon.model.CouponSortType
import com.conkeep.ui.feature.coupon.model.CouponUiModel
import com.conkeep.ui.theme.ConKeepTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CouponScreen(
    backStack: NavBackStack<NavKey>,
    viewModel: CouponListViewModel = hiltViewModel(),
) {
    val coupons: LazyPagingItems<CouponUiModel> = viewModel.coupons.collectAsLazyPagingItems()
    val couponCountHeaderState by viewModel.couponCountHeaderState.collectAsStateWithLifecycle()
    val couponCountSummary by viewModel.couponCountSummary.collectAsStateWithLifecycle()
    val queryConfig by viewModel.queryConfig.collectAsStateWithLifecycle()
    var isFilterChipExpanded by rememberSaveable { mutableStateOf(true) }
    var typingQuery: String by rememberSaveable { mutableStateOf("") }

    var prevFilter by rememberSaveable { mutableStateOf(queryConfig.filter) }
    var prevSort by rememberSaveable { mutableStateOf(queryConfig.sort) }

    val listState = rememberLazyListState()

    // 새 쿠폰 추가 이벤트 수신시 스크롤 최근 등록순 전체로 필터를 변경 후, 쿠폰이 추가될 때까지 대기 한 뒤 최상단으로 이동
    LaunchedEffect(viewModel) {
        viewModel.couponAddedEvent.collectLatest { couponId ->
            Log.d("CouponScreen", "새 쿠폰 추가: $couponId")

            runCatching {
                typingQuery = ""
                viewModel.readyToShowNewCoupon()

                val found =
                    withTimeoutOrNull(3000) {
                        snapshotFlow { coupons.peek(0)?.id }
                            .first { it == couponId }
                    }

                if (found != null) {
                    Log.d("CouponScreen", "쿠폰 $couponId 발견")
                    listState.animateScrollToItem(0)
                } else {
                    Log.w("CouponScreen", "타임아웃, 강제 스크롤")
                    listState.animateScrollToItem(0)
                }
            }.onFailure { e ->
                Log.e("CouponScreen", "쿠폰 추가 스크롤 실패", e)
            }
        }
    }

    // 일반적인 필터/정렬 클릭 변경 시 스크롤 최상단 이동
    LaunchedEffect(queryConfig.filter, queryConfig.sort) {
        val filterChanged = queryConfig.filter != prevFilter
        val sortChanged = queryConfig.sort != prevSort

        if (filterChanged || sortChanged) {
            Log.d(
                "CouponScreen",
                "필터/정렬 변경: $prevFilter→${queryConfig.filter}, $prevSort→${queryConfig.sort}",
            )
            prevFilter = queryConfig.filter
            prevSort = queryConfig.sort
            listState.animateScrollToItem(0)
        }
    }

    val pickMedia: ManagedActivityResultLauncher<PickVisualMediaRequest, Uri?> =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia(),
        ) { uri ->
            uri?.let {
                typingQuery = ""
                viewModel.addCouponFromUri(uri)
            }
        }

    CouponScreenContent(
        coupons = coupons,
        typingQuery = typingQuery,
        onTypingQueryUpdate = { typingQuery = it },
        listState = listState,
        couponCountHeaderState = couponCountHeaderState,
        selectedSortType = queryConfig.sort,
        couponFilterType = queryConfig.filter,
        couponCountSummary = couponCountSummary,
        isFilterExpanded = isFilterChipExpanded,
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
        onCouponSortClick = viewModel::toggleCouponSortType,
        onFilterTypeClick = viewModel::changeCouponFilterType,
        onSearchTriggered = viewModel::searchCoupons,
        onFilterChipExpandClick = {
            isFilterChipExpanded = !isFilterChipExpanded
        },
        onClearSearchQuery = {
            typingQuery = ""
            viewModel.clearSearchKeyword()
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CouponScreenContent(
    coupons: LazyPagingItems<CouponUiModel>,
    typingQuery: String,
    onTypingQueryUpdate: (String) -> Unit,
    listState: LazyListState,
    couponCountHeaderState: CouponCountHeaderState,
    isFilterExpanded: Boolean = false,
    onCouponAddClick: () -> Unit,
    onCouponDetailClick: (String) -> Unit,
    onCouponSortClick: () -> Unit,
    onSearchTriggered: (String) -> Unit,
    onFilterChipExpandClick: () -> Unit,
    onClearSearchQuery: () -> Unit,
    couponFilterType: CouponFilterType = CouponFilterType.ALL,
    couponCountSummary: CouponCountSummary = CouponCountSummary(),
    selectedSortType: CouponSortType = CouponSortType.RECENT_ADD,
    onFilterTypeClick: (CouponFilterType) -> Unit,
) {
    val focusManager = LocalFocusManager.current

    Scaffold(
        topBar = {
            TopBar(
                onClickAdd = {
                    focusManager.clearFocus()
                    onCouponAddClick()
                },
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = {
                            focusManager.clearFocus()
                        })
                    },
        ) {
            Spacer(modifier = Modifier.padding(top = 16.dp))
            SearchBar(
                query = typingQuery,
                onQueryUpdate = {
                    onTypingQueryUpdate(it)
                    onSearchTriggered(it.trim())
                },
                onSearch = {
                    onSearchTriggered(typingQuery.trim())
                    focusManager.clearFocus()
                },
                onClearQuery = {
                    onClearSearchQuery()
                    focusManager.clearFocus()
                },
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )

            CouponSortRow(
                isSearched = couponCountHeaderState.isSearchActive,
                count = couponCountHeaderState.totalCount,
                selectFilterType = couponFilterType,
                selectedSort = selectedSortType,
                isFilterExpanded = isFilterExpanded,
                onFilterChipExpandClick = onFilterChipExpandClick,
                onSortClick = onCouponSortClick,
                modifier = Modifier.padding(horizontal = 20.dp),
            )

            if (isFilterExpanded) {
                CouponFilterChipRow(
                    selectedFilter = couponFilterType,
                    couponCountSummary = couponCountSummary,
                    onFilterSelected = onFilterTypeClick,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
            ) {
                items(
                    count = coupons.itemCount,
                    key = coupons.itemKey { it.id },
                ) { index ->
                    val coupon = coupons[index]
                    if (coupon != null) {
                        CouponCard(
                            couponUiModel = coupon,
                            onClick = { onCouponDetailClick(coupon.id) },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                    } else {
                        Text("로딩중") // TODO: 로딩 스켈레톤 UI
                    }
                }
            }
        }
    }
}

private val dummyCoupons =
    listOf(
        CouponUiModel(
            id = "0",
            number = "1234-5678-9012",
            name = "스타벅스 아이스 아메리카노 T",
            brand = "스타벅스",
            expiryDate = LocalDate.parse("2025-12-31"),
            isUsed = false,
            isExpired = false,
            localStatus = CouponLocalStatus.RECOGNIZED,
        ),
        CouponUiModel(
            id = "1",
            number = "1234-5678-3333",
            name = "스타벅스 아이스 콜드부루 T",
            brand = "스타벅스",
            expiryDate = LocalDate.parse("2000-12-31"),
            isUsed = false,
            isExpired = true,
            localStatus = CouponLocalStatus.RECOGNIZED,
        ),
        CouponUiModel(
            id = "2",
            number = "9876-5432-1098",
            name = "배스킨라빈스 싱글레귤러",
            brand = "배스킨라빈스",
            expiryDate = LocalDate.parse("2025-12-15"),
            isUsed = false,
            isExpired = false,
            localStatus = CouponLocalStatus.PREPROCESSED,
        ),
        CouponUiModel(
            id = "3",
            number = "1111-2222-3333",
            name = "네이버페이 1만원권",
            brand = "네이버페이",
            expiryDate = LocalDate.parse("2026-01-20"),
            isUsed = true,
            isExpired = false,
            isMonetary = true,
            amount = 10000,
            localStatus = CouponLocalStatus.AI_FAILED,
        ),
        CouponUiModel(
            id = "4",
            number = "5555-6666-7777",
            name = "교촌치킨 허니콤보 웨지감자 세트",
            brand = "교촌치킨",
            expiryDate = LocalDate.parse("2026-02-10"),
            isUsed = false,
            isExpired = false,
            localStatus = CouponLocalStatus.PENDING,
        ),
    )

@Preview(showBackground = true)
@Composable
private fun CouponScreenContentPreview() {
    val pagingDataFlow = flowOf(PagingData.from(dummyCoupons))
    val dummyPagingItems = pagingDataFlow.collectAsLazyPagingItems()
    ConKeepTheme(darkTheme = false) {
        CouponScreenContent(
            coupons = dummyPagingItems,
            typingQuery = "",
            onTypingQueryUpdate = {},
            listState = rememberLazyListState(),
            couponCountHeaderState = CouponCountHeaderState(),
            isFilterExpanded = true,
            onCouponAddClick = {},
            onCouponDetailClick = {},
            onCouponSortClick = {},
            onSearchTriggered = {},
            couponFilterType = CouponFilterType.ALL,
            selectedSortType = CouponSortType.RECENT_ADD,
            couponCountSummary = couponCountSummaryFixture,
            onFilterChipExpandClick = {},
            onFilterTypeClick = {},
            onClearSearchQuery = {},
        )
    }
}
