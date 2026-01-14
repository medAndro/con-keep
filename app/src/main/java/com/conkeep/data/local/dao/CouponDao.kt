package com.conkeep.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.conkeep.data.local.entity.CouponEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CouponDao {
    @Query("SELECT * FROM coupons WHERE id = :id")
    fun getCouponFlow(id: String): Flow<CouponEntity?>

    /**
     * 검색 쿼리
     * 검색어(searchQuery)가 비어있으면 전체 목록을 반환하고,
     * 검색어가 존재하면 상품명, 브랜드, 쿠폰 번호 중 하나라도 포함하는 쿠폰 목록을 반환합니다.
     */
    @Query(
        """
        SELECT * FROM coupons 
        WHERE user_id = :userId 
        AND (
            :searchQuery = '' OR 
            product_name LIKE '%' || :searchQuery || '%' OR 
            brand LIKE '%' || :searchQuery || '%' OR 
            coupon_pin LIKE '%' || :searchQuery || '%'
        )
        ORDER BY created_at DESC
    """,
    )
    fun searchCouponsPaging(
        userId: String,
        searchQuery: String,
    ): PagingSource<Int, CouponEntity>

    /**
     * [통합 검색/필터/정렬 쿼리]
     * @param userId: 사용자 ID
     * @param searchQuery 검색어, 비어있다면 전체 반환, 존재하면 상품명, 브랜드, 쿠폰 번호에서 검색
     * @param today: 현재 날짜 (ISO 8601 형식: "YYYY-MM-DD")
     * @param filterType: 0(전체), 1(사용가능), 2(사용완료), 3(기간만료)
     * @param sortType: 0(최근 등록순), 1(만료 임박순)
     */
    @Query(
        """
        SELECT * FROM coupons 
        WHERE user_id = :userId 
        AND (
            :searchQuery = '' OR 
            product_name LIKE '%' || :searchQuery || '%' OR 
            brand LIKE '%' || :searchQuery || '%' OR 
            coupon_pin LIKE '%' || :searchQuery || '%'
        )
        AND (
            CASE 
                WHEN :filterType = 1 THEN is_used = 0 AND expiry_date >= :today -- 사용가능
                WHEN :filterType = 2 THEN is_used = 1 -- 사용완료
                WHEN :filterType = 3 THEN is_used = 0 AND expiry_date < :today -- 기간만료
                ELSE 1 -- 전체 (filterType = 0)
            END
        )
        ORDER BY 
            -- 1. 전체 보기(filterType=0)일 때 상태 우선순위: 사용가능(0) > 사용완료(1) > 기간만료(2)
            CASE 
                WHEN :filterType = 0 THEN 
                    CASE 
                        WHEN is_used = 0 AND expiry_date >= :today THEN 0
                        WHEN is_used = 1 THEN 1
                        ELSE 2
                    END
                ELSE 0 
            END ASC,
            
            -- 2. 실제 정렬 조건 (최근등록순 또는 만료임박순)
            CASE WHEN :sortType = 0 THEN created_at END DESC,
            CASE WHEN :sortType = 1 THEN expiry_date END ASC,
            
            -- 3. 동일 조건 시 최종 정렬
            id DESC
    """,
    )
    fun searchCouponsPaging(
        userId: String,
        searchQuery: String,
        today: String,
        filterType: Int,
        sortType: Int,
    ): PagingSource<Int, CouponEntity>

    @Query("SELECT * FROM coupons WHERE user_id = :userId AND is_used = 0 ORDER BY expiry_date ASC")
    fun getActiveCoupons(userId: String): Flow<List<CouponEntity>>

    @Query("SELECT * FROM coupons WHERE id = :id")
    suspend fun getCouponById(id: String): CouponEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(coupon: CouponEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(coupons: List<CouponEntity>)

    @Update
    suspend fun update(coupon: CouponEntity)

    @Query("UPDATE coupons SET image_url = :imageUrl, image_key = :imageKey WHERE id = :id")
    suspend fun updateR2Info(
        id: String,
        imageUrl: String,
        imageKey: String,
    )

    @Query(
        """
    UPDATE OR IGNORE coupons 
    SET 
        product_name = COALESCE(:productName, product_name),
        brand = COALESCE(:brand, brand),
        coupon_pin = COALESCE(:couponPin, coupon_pin),
        expiry_date = COALESCE(:expiryDate, expiry_date),
        is_monetary = COALESCE(:isMonetary, is_monetary),
        amount = COALESCE(:amount, amount),
        category = COALESCE(:category, category),
        local_status = COALESCE(:localStatus, local_status),
        
        updated_at = :updatedAt
    WHERE id = :couponId
""",
    )
    suspend fun updateAiRecognitionInfo(
        couponId: String,
        productName: String?,
        brand: String?,
        couponPin: String?,
        expiryDate: String?,
        isMonetary: Boolean?,
        amount: Int?,
        category: String?,
        localStatus: String?,
        updatedAt: Long,
    ): Int

    @Query("UPDATE coupons SET is_used = 1, used_at = :usedAt WHERE id = :id")
    suspend fun markAsUsed(
        id: String,
        usedAt: Long,
    )

    @Delete
    suspend fun delete(coupon: CouponEntity)

    @Query("DELETE FROM coupons WHERE user_id = :userId")
    suspend fun deleteAllByUser(userId: String)
}
