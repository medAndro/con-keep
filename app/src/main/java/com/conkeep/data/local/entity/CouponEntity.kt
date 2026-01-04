package com.conkeep.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "coupons")
data class CouponEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "remote_id")
    val remoteId: String?,
    @ColumnInfo(name = "user_id")
    val userId: String,
    // 업로드된 이미지 정보 (Supabase와 동기화)
    @ColumnInfo(name = "image_url")
    val imageUrl: String?,
    @ColumnInfo(name = "image_key")
    val imageKey: String?,
    @ColumnInfo(name = "thumbnail_url")
    val thumbnailUrl: String?,
    // 로컬 전용 컬럼 (동기화 안 함)
    @ColumnInfo(name = "local_image_path")
    val localImagePath: String?, // 앱 파일 저장소 경로
    // 쿠폰 정보
    @ColumnInfo(name = "product_name")
    val productName: String?,
    val brand: String?,
    @ColumnInfo(name = "coupon_pin")
    val couponPin: String?,
    // ISO 8601 형식 (2026-02-01)
    @ColumnInfo(name = "expiry_date")
    val expiryDate: LocalDate?,
    @ColumnInfo(name = "is_monetary")
    val isMonetary: Boolean,
    // nullable (금액 쿠폰이 아닐 경우)
    val amount: Int?,
    // enum을 String으로 저장
    val category: String?,
    @ColumnInfo(name = "user_memo")
    val userMemo: String?,
    @ColumnInfo(name = "is_used")
    val isUsed: Boolean,
    // timestamptz를 Unix timestamp로 저장
    @ColumnInfo(name = "used_at")
    val usedAt: Long?,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
    // 동기화 상태 추적
    @ColumnInfo(name = "is_synced")
    val isSynced: Boolean = false,
    // 로컬 UI 상태
    @ColumnInfo(name = "local_status")
    val localStatus: CouponLocalStatus? = CouponLocalStatus.PENDING,
)
