// data/remote/dto/DeviceDto.kt
package com.conkeep.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeviceDto(
    val id: String? = null, // 생성 시 null (DB 자동 생성)
    @SerialName("user_id") val userId: String,
    @SerialName("fcm_token") val fcmToken: String,
    @SerialName("last_used_at") val lastUsedAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)
