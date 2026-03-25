package com.conkeep.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class DeleteCouponsResponse(
    val success: Boolean,
    val deletedCount: Int,
    val message: String,
)
