package com.conkeep.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class AiAnalyzeRequest(
    val couponId: String,
    val imageUrl: String,
    val barcode: String? = null,
)
