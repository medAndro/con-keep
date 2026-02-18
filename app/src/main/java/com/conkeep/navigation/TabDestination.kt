package com.conkeep.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.conkeep.R

enum class TabDestination(
    val route: Route,
    @param:StringRes val titleRes: Int,
    @param:DrawableRes val iconRes: Int,
) {
    Coupon(
        route = Route.CouponScreen,
        titleRes = R.string.bottom_nav_coupon_title,
        iconRes = R.drawable.ic_ticket,
    ),
    Setting(
        route = Route.SettingScreen,
        titleRes = R.string.bottom_nav_setting_title,
        iconRes = R.drawable.ic_setting,
    ),
    ;

    companion object {
        val items = entries.toList()

        fun fromRoute(route: Route): TabDestination? = entries.find { it.route::class == route::class }
    }
}
