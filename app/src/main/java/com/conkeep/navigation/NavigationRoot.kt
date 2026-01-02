package com.conkeep.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.conkeep.ui.feature.coupon.CouponScreen
import com.conkeep.ui.feature.coupon.detail.CouponDetailScreen
import com.conkeep.ui.feature.coupon.detail.CouponDetailViewModel

@Composable
fun NavigationRoot() {
    val backStack = rememberNavBackStack(Route.CouponScreen)

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators =
            listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
        entryProvider =
            entryProvider {
                entry<Route.CouponScreen> {
                    CouponScreen(backStack = backStack)
                }

                entry<Route.CouponDetailScreen> { key ->
                    val viewModel =
                        hiltViewModel<CouponDetailViewModel, CouponDetailViewModel.Factory> { factory ->
                            factory.create(key.id)
                        }

                    CouponDetailScreen(
                        id = key.id,
                        backStack = backStack,
                        viewModel = viewModel,
                    )
                }
            },
    )
}
