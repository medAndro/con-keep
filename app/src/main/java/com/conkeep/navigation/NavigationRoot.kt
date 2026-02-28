package com.conkeep.navigation

import androidx.compose.runtime.Composable
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

@Composable
fun NavigationRoot(initialRoute: Route) {
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
            MainNavigation()
        }
    }
}

@Composable
private fun MainNavigation() {
    val couponBackStack = rememberNavBackStack(Route.CouponScreen)
    val settingBackStack = rememberNavBackStack(Route.SettingScreen)

    var activeTab by rememberSaveable {
        mutableStateOf(TabDestination.Coupon)
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
                        id = key.id,
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
                    )
                }
            },
    )
}
