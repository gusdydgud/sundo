package com.example.liststart.entity

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.*

@Dao
interface MarkerDao {

    @Query("SELECT * FROM marker WHERE bno = :bno")
    suspend fun getAllMarkersById(bno: Long): List<MarkerEntity> // 올바른 엔티티 타입을 반환해야 합니다.

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMarker(marker: MarkerEntity): Long // Entity 삽입, 삽입된 row의 ID 반환

    @Update
    suspend fun updateMarker(marker: MarkerEntity): Int // 업데이트된 row의 개수를 반환

    @Query("DELETE FROM marker WHERE mno = :mno")
    suspend fun deleteMarkerById(mno: Long): Int // 삭제된 row의 개수를 반환

    @Query("SELECT * FROM marker WHERE mno = :mno LIMIT 1")
    suspend fun getMarkerById(mno: Long): MarkerEntity?
}
