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
    data object SettingScreen : Route

    @Serializable
    data object NoticeScreen : Route

    @Serializable
    data object DeleteAccountScreen : Route

    @Serializable
    data class CouponDetailScreen(
        val id: String,
    ) : Route

    @Serializable
    data class CouponEditScreen(
        val id: String,
    ) : Route

    @Serializable
    data class CouponImageScreen(
        val id: String,
    ) : Route
}
