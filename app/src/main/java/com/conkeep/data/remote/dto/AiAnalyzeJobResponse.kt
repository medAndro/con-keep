package com.conkeep.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class AiAnalyzeJobResponse(
    val success: Boolean = false,
    val message: String = "",
    val error: String? = null,
    val metadata: AiMetadata? = null,
)

@Serializable
data class AiMetadata(
    val userId: String,
    val couponId: String,
)
