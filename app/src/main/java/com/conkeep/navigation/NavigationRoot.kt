package com.conkeep.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.conkeep.ui.feature.auth.LoginScreen
import com.conkeep.ui.feature.coupon.detail.CouponDetailScreen
import com.conkeep.ui.feature.coupon.detail.CouponDetailViewModel
import com.conkeep.ui.feature.coupon.edit.CouponEditScreen
import com.conkeep.ui.feature.coupon.edit.CouponEditViewModel
import com.conkeep.ui.feature.coupon.image.CouponImageScreen
import com.conkeep.ui.feature.coupon.image.CouponImageViewModel
import com.conkeep.ui.feature.coupon.list.CouponScreen
import com.conkeep.ui.feature.setting.SettingScreen
import com.conkeep.ui.feature.setting.normal.notice.NoticeScreen

@Composable
fun NavigationRoot(
    initialRoute: Route,
    pendingCouponId: String?,
    onDeepLinkHandled: () -> Unit,
) {
    var isLoggedIn by rememberSaveable {
        mutableStateOf(initialRoute != Route.LoginScreen)
    }
    when {
        !isLoggedIn -> {
            LoginScreen(
                onLoginSuccess = {
                    isLoggedIn = true
                },
            )
        }

        else -> {
            MainNavigation(
                pendingCouponId = pendingCouponId,
                onDeepLinkHandled = onDeepLinkHandled,
                navigateLoginScreen = {
                    isLoggedIn = false
                },
            )
        }
    }
}

@Composable
private fun MainNavigation(
    pendingCouponId: String?,
    onDeepLinkHandled: () -> Unit,
    navigateLoginScreen: () -> Unit,
) {
    val couponBackStack = rememberNavBackStack(Route.CouponScreen)
    val settingBackStack = rememberNavBackStack(Route.SettingScreen)

    var activeTab by rememberSaveable {
        mutableStateOf(TabDestination.Coupon)
    }

    // [딥링크 감지 로직]
    LaunchedEffect(pendingCouponId) {
        if (pendingCouponId != null) {
            // 1. 탭을 쿠폰 탭으로 강제 이동
            activeTab = TabDestination.Coupon

            // 2. 이미 상세 페이지가 열려있을 수 있으므로 중복 방지 처리를 하며 상세 페이지 추가
            // Navigation3는 BackStack(List)에 Key를 추가하면 바로 화면이 이동합니다.
            val route = Route.CouponDetailScreen(pendingCouponId)

            // 현재 스택의 마지막이 해당 쿠폰 상세가 아닐 때만 추가
            if (couponBackStack.lastOrNull() != route) {
                couponBackStack.add(route)
            }

            // 3. 처리가 완료되었음을 Activity에 알림
            onDeepLinkHandled()
        }
    }

    TabContainer(
        currentTab = activeTab,
        tabs = TabDestination.entries,
    ) { tab ->
        when (tab) {
            TabDestination.Coupon -> {
                CouponNavigation(
                    couponBackStack = couponBackStack,
                    onTabChange = { activeTab = it },
                )
            }

            TabDestination.Setting -> {
                SettingNavigation(
                    settingBackStack = settingBackStack,
                    onTabChange = { activeTab = it },
                    moveLoginScreen = {
                        activeTab = TabDestination.Coupon
                        navigateLoginScreen()
                    },
                )
            }
        }
    }
}

@Composable
private fun CouponNavigation(
    couponBackStack: NavBackStack<NavKey>,
    onTabChange: (TabDestination) -> Unit,
) {
    NavDisplay(
        backStack = couponBackStack,
        onBack = {
            if (couponBackStack.size > 1) {
                couponBackStack.removeLastOrNull()
            }
        },
        entryDecorators =
            listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
        entryProvider =
            entryProvider {
                entry<Route.CouponScreen> {
                    CouponScreen(
                        couponBackStack = couponBackStack,
                        onTabChange = onTabChange,
                    )
                }

                entry<Route.CouponDetailScreen> { key ->
                    val viewModel =
                        hiltViewModel<CouponDetailViewModel, CouponDetailViewModel.Factory> { factory ->
                            factory.create(key.id)
                        }
                    CouponDetailScreen(
                        backStack = couponBackStack,
                        viewModel = viewModel,
                    )
                }

                entry<Route.CouponEditScreen> { key ->
                    val viewModel =
                        hiltViewModel<CouponEditViewModel, CouponEditViewModel.Factory> { factory ->
                            factory.create(key.id)
                        }
                    CouponEditScreen(
                        backStack = couponBackStack,
                        viewModel = viewModel,
                    )
                }

                entry<Route.CouponImageScreen> { key ->
                    val viewModel =
                        hiltViewModel<CouponImageViewModel, CouponImageViewModel.Factory> { factory ->
                            factory.create(key.id)
                        }
                    CouponImageScreen(
                        backStack = couponBackStack,
                        viewModel = viewModel,
                    )
                }
            },
    )
}

@Composable
private fun SettingNavigation(
    settingBackStack: NavBackStack<NavKey>,
    onTabChange: (TabDestination) -> Unit,
    moveLoginScreen: () -> Unit,
) {
    NavDisplay(
        backStack = settingBackStack,
        onBack = {
            if (settingBackStack.size > 1) {
                settingBackStack.removeLastOrNull()
            }
        },
        entryDecorators =
            listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
        entryProvider =
            entryProvider {
                entry<Route.SettingScreen> {
                    SettingScreen(
                        settingBackStack = settingBackStack,
                        onTabChange = onTabChange,
                        moveLoginScreen = moveLoginScreen,
                    )
                }
                entry<Route.NoticeScreen> {
                    NoticeScreen(
                        settingBackStack = settingBackStack,
                        onTabChange = onTabChange,
                    )
                }
            },
    )
}
