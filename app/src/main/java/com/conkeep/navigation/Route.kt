package com.conkeep.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface Route : NavKey {
    @Serializable
    data object LoginScreen : Route

    @Serializable
    data object CouponScreen : Route

    @Serializable
    data class CouponDetailScreen(
        val id: String,
    ) : Route
}
