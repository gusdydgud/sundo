package com.example.liststart.entity

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.*

@Dao
interface BusinessDao {

    @Query("SELECT * FROM business ORDER BY bno DESC")
    suspend fun getAllBusinesses(): List<BusinessEntity> // 올바른 엔티티 타입을 반환해야 합니다.

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBusiness(business: BusinessEntity): Long // Entity 삽입, 삽입된 row의 ID 반환

    @Update
    suspend fun updateBusiness(business: BusinessEntity): Int // 업데이트된 row의 개수를 반환

    @Query("DELETE FROM business WHERE bno IN (:ids)")
    suspend fun deleteBusinessesByIds(ids: List<Long>): Int // 삭제된 row의 개수를 반환

    @Query("SELECT * FROM business WHERE bno = :bno LIMIT 1")
    suspend fun getBusinessById(bno: Long): BusinessEntity? // Null을 반환할 수 있으므로 Nullable로 처리
}
