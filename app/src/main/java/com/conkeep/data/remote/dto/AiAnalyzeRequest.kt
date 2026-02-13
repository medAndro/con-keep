package com.conkeep.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class AiAnalyzeRequest(
    val couponId: String,
    val imageUrl: String,
    val barcode: String? = null,
    val createdAt: String, // "2026-02-13T11:32:27Z" 형식
)
