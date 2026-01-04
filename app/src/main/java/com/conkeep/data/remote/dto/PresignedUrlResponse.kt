package com.conkeep.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PresignedUrlResponse(
    @SerialName("uploadUrl") val uploadUrl: String,
    @SerialName("imageUrl") val imageUrl: String,
)
