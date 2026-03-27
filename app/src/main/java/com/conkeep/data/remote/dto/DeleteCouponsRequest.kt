package com.conkeep.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class DeleteCouponsRequest(
    val couponIds: List<String>,
)
