package com.conkeep.ui.feature.coupon.list

import android.net.Uri
import android.util.Log
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.conkeep.R
import com.conkeep.data.local.entity.CouponStatus
import com.conkeep.navigation.Route
import com.conkeep.navigation.TabDestination
import com.conkeep.ui.component.BottomNavigationBar
import com.conkeep.ui.component.MainTopBar
import com.conkeep.ui.feature.coupon.list.component.CouponCard
import com.conkeep.ui.feature.coupon.list.component.CouponFilterChipRow
import com.conkeep.ui.feature.coupon.list.component.CouponSortRow
import com.conkeep.ui.feature.coupon.list.component.SearchBar
import com.conkeep.ui.feature.coupon.list.component.ShimmerCouponCard
import com.conkeep.ui.feature.coupon.list.component.couponCountSummaryFixture
import com.conkeep.ui.feature.coupon.model.CouponCountHeaderState
import com.conkeep.ui.feature.coupon.model.CouponCountSummary
import com.conkeep.ui.feature.coupon.model.CouponFilterType
import com.conkeep.ui.feature.coupon.model.CouponSortType
import com.conkeep.ui.feature.coupon.model.CouponUiModel
import com.conkeep.ui.theme.ConKeepTheme
import com.conkeep.ui.theme.PretendardMedium16
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CouponScreen(
    couponBackStack: NavBackStack<NavKey>,
    onTabChange: (TabDestination) -> Unit,
    viewModel: CouponListViewModel = hiltViewModel(),
) {
    val coupons: LazyPagingItems<CouponUiModel> = viewModel.coupons.collectAsLazyPagingItems()
    var hasLoadedOnce by remember { mutableStateOf(false) }
    LaunchedEffect(coupons.loadState.source.refresh) {
        if (coupons.loadState.source.refresh is LoadState.NotLoading) {
            hasLoadedOnce = true
        }
    }
    val showInitialShimmer =
        coupons.loadState.source.refresh is LoadState.Loading &&
            !hasLoadedOnce
    val isRefreshing = coupons.loadState.source.refresh is LoadState.Loading

    val couponCountHeaderState by viewModel.couponCountHeaderState.collectAsStateWithLifecycle()
    val couponCountSummary by viewModel.couponCountSummary.collectAsStateWithLifecycle()
    val queryConfig by viewModel.queryConfig.collectAsStateWithLifecycle()
    var isFilterChipExpanded by rememberSaveable { mutableStateOf(true) }
    var typingQuery: String by rememberSaveable { mutableStateOf("") }

    var prevFilter by rememberSaveable { mutableStateOf(queryConfig.filter) }
    var prevSort by rememberSaveable { mutableStateOf(queryConfig.sort) }

    val listState = rememberLazyListState()

    val placeholderPainter = painterResource(R.drawable.img_conkeep_placeholder)

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
        showInitialShimmer = showInitialShimmer,
        isRefreshing = isRefreshing,
        placeholderPainter = placeholderPainter,
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
            couponBackStack.add(Route.CouponDetailScreen(id = couponId))
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
        onTabChange = onTabChange,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CouponScreenContent(
    coupons: LazyPagingItems<CouponUiModel>,
    showInitialShimmer: Boolean,
    isRefreshing: Boolean,
    placeholderPainter: Painter,
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
    onTabChange: (TabDestination) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    var isSearchBarShow by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            MainTopBar(
                onClickSearchBarToggle = {
                    isSearchBarShow = !isSearchBarShow
                },
                onClickAdd = {
                    focusManager.clearFocus()
                    onCouponAddClick()
                },
            )
        },
        bottomBar = {
            BottomNavigationBar(
                currentTab = TabDestination.Coupon,
                onTabChange = onTabChange,
                onTabReselect = {},
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
            AnimatedVisibility(
                visible = isSearchBarShow,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
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
                    modifier =
                        Modifier.padding(
                            top = 20.dp,
                            start = 24.dp,
                            end = 24.dp,
                        ),
                )
            }

            CouponSortRow(
                isSearched = couponCountHeaderState.isSearchActive,
                count = couponCountHeaderState.totalCount,
                selectFilterType = couponFilterType,
                selectedSort = selectedSortType,
                isFilterExpanded = isFilterExpanded,
                onFilterChipExpandClick = onFilterChipExpandClick,
                onSortClick = onCouponSortClick,
                modifier = Modifier.padding(top = 8.dp, start = 20.dp, end = 20.dp),
            )

            AnimatedVisibility(
                visible = isFilterExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                CouponFilterChipRow(
                    selectedFilter = couponFilterType,
                    couponCountSummary = couponCountSummary,
                    onFilterSelected = onFilterTypeClick,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
            }
            when {
                // 최초 로딩 시에만 전체 shimmer 표시
                showInitialShimmer -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.weight(1f),
                    ) {
                        items(count = 10) {
                            ShimmerCouponCard(
                                modifier =
                                    Modifier.padding(
                                        horizontal = 16.dp,
                                        vertical = 4.dp,
                                    ),
                            )
                        }
                    }
                }

                // 로딩 완료 후 아이템 없음 = 진짜 빈 상태
                // isRefreshing 중에는 표시하지 않아 빈 화면 깜빡임 방지
                coupons.itemCount == 0 && !isRefreshing -> {
                    CouponEmptyContent(placeholderPainter, couponFilterType)
                }

                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.weight(1f),
                    ) {
                        items(
                            count = coupons.itemCount,
                            key = coupons.itemKey { it.id },
                        ) { index ->
                            val coupon = coupons[index]
                            when {
                                coupon != null -> {
                                    CouponCard(
                                        couponUiModel = coupon,
                                        onClick = { onCouponDetailClick(coupon.id) },
                                        modifier =
                                            Modifier.padding(
                                                horizontal = 16.dp,
                                                vertical = 4.dp,
                                            ),
                                    )
                                }

                                else -> {
                                    // append 페이징 중 null 플레이스홀더에 개별 shimmer
                                    ShimmerCouponCard(
                                        modifier =
                                            Modifier.padding(
                                                horizontal = 16.dp,
                                                vertical = 4.dp,
                                            ),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CouponEmptyContent(
    placeholderPainter: Painter,
    couponFilterType: CouponFilterType,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Image(
                painter = placeholderPainter,
                contentDescription = "쿠폰이 없습니다",
                modifier =
                    Modifier
                        .fillMaxWidth(0.5f),
                contentScale = ContentScale.FillWidth,
            )
            when (couponFilterType) {
                CouponFilterType.ALL -> {
                    Text(
                        stringResource(R.string.coupon_list_screen_empty_all_placeholder_text),
                        textAlign = TextAlign.Center,
                        style = PretendardMedium16,
                    )
                }

                CouponFilterType.AVAILABLE -> {
                    Text(
                        stringResource(R.string.coupon_list_screen_empty_available_placeholder_text),
                        textAlign = TextAlign.Center,
                        style = PretendardMedium16,
                    )
                }

                CouponFilterType.USED -> {
                    Text(
                        stringResource(R.string.coupon_list_screen_empty_used_placeholder_text),
                        textAlign = TextAlign.Center,
                        style = PretendardMedium16,
                    )
                }

                CouponFilterType.EXPIRED -> {
                    Text(
                        stringResource(R.string.coupon_list_screen_empty_expired_placeholder_text),
                        textAlign = TextAlign.Center,
                        style = PretendardMedium16,
                    )
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
            status = CouponStatus.SUCCESS,
        ),
        CouponUiModel(
            id = "1",
            number = "1234-5678-3333",
            name = "스타벅스 아이스 콜드부루 T",
            brand = "스타벅스",
            expiryDate = LocalDate.parse("2000-12-31"),
            isUsed = false,
            isExpired = true,
            status = CouponStatus.SUCCESS,
        ),
        CouponUiModel(
            id = "2",
            number = "9876-5432-1098",
            name = "배스킨라빈스 싱글레귤러",
            brand = "배스킨라빈스",
            expiryDate = LocalDate.parse("2025-12-15"),
            isUsed = false,
            isExpired = false,
            status = CouponStatus.ANALYZING,
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
            status = CouponStatus.AI_FAILED,
        ),
        CouponUiModel(
            id = "4",
            number = "5555-6666-7777",
            name = "교촌치킨 허니콤보 웨지감자 세트",
            brand = "교촌치킨",
            expiryDate = LocalDate.parse("2026-02-10"),
            isUsed = false,
            isExpired = false,
            status = CouponStatus.PENDING,
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
            showInitialShimmer = false,
            isRefreshing = false,
            placeholderPainter = painterResource(R.drawable.img_conkeep_placeholder),
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
            onTabChange = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CouponScreenContentEmptyPreview() {
    val pagingDataFlow = flowOf(PagingData.from(listOf<CouponUiModel>()))
    val dummyPagingItems = pagingDataFlow.collectAsLazyPagingItems()
    ConKeepTheme(darkTheme = false) {
        CouponScreenContent(
            coupons = dummyPagingItems,
            showInitialShimmer = false,
            isRefreshing = false,
            placeholderPainter = painterResource(R.drawable.img_conkeep_placeholder),
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
            onTabChange = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CouponScreenContentShimmerPreview() {
    val pagingDataFlow = flowOf(PagingData.from(listOf<CouponUiModel>()))
    val dummyPagingItems = pagingDataFlow.collectAsLazyPagingItems()
    ConKeepTheme(darkTheme = false) {
        CouponScreenContent(
            coupons = dummyPagingItems,
            showInitialShimmer = true,
            isRefreshing = true,
            placeholderPainter = painterResource(R.drawable.img_conkeep_placeholder),
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
            onTabChange = {},
        )
    }
}
