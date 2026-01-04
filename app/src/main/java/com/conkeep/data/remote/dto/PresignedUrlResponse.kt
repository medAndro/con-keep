package com.conkeep.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PresignedUrlResponse(
    @SerialName("uploadUrl") val uploadPresignedUrl: String,
    @SerialName("imageUrl") val imageUrl: String,
    @SerialName("key") val r2ObjectKey: String,
)
