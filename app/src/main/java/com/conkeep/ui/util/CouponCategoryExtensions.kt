// ui/util/CouponCategoryExtensions.kt
package com.conkeep.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.conkeep.R
import com.conkeep.domain.model.CouponCategory

val CouponCategory.displayNameRes: Int
    get() =
        when (this) {
            CouponCategory.CAFE -> R.string.category_cafe
            CouponCategory.CONVENIENCE_STORE -> R.string.category_convenience_store
            CouponCategory.MART -> R.string.category_mart
            CouponCategory.BAKERY -> R.string.category_bakery
            CouponCategory.CHICKEN_PIZZA -> R.string.category_chicken_pizza
            CouponCategory.BURGER_SANDWICH -> R.string.category_burger_sandwich
            CouponCategory.FAST_FOOD -> R.string.category_fast_food
            CouponCategory.DINING -> R.string.category_dining
            CouponCategory.ICE_CREAM -> R.string.category_ice_cream
            CouponCategory.DESSERT -> R.string.category_dessert
            CouponCategory.DEPARTMENT_STORE -> R.string.category_department_store
            CouponCategory.GIFT_CARD -> R.string.category_gift_card
            CouponCategory.CULTURE -> R.string.category_culture
            CouponCategory.FASHION_BEAUTY -> R.string.category_fashion_beauty
            CouponCategory.GAS_TRANSPORT -> R.string.category_gas_transport
            CouponCategory.ETC -> R.string.category_etc
        }

@Composable
fun CouponCategory.getDisplayName(): String = stringResource(displayNameRes)
