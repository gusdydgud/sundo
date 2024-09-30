package com.example.liststart.datasource


import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.liststart.model.Marker

@Dao
interface MarkerDao {

    @Query("SELECT * FROM marker WHERE bno = :bno")
    fun getMarkersForBusiness(bno: Long): LiveData<List<Marker>> // LiveData 반환

    @Query("SELECT * FROM marker WHERE bno = :bno")
    fun getMarkersForBusinessSync(bno: Long): List<Marker> // 동기식 메서드

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addMarker(marker: Marker): Long // 마커 추가 또는 업데이트

    @Delete
    suspend fun deleteMarkers(markers: List<Marker>): Int // 마커 삭제

    @Update
    suspend fun updateMarker(marker: Marker): Int // 마커 업데이트
    // mno와 bno로 특정 마커를 조회하는 메서드 추가
    @Query("SELECT * FROM marker WHERE mno = :mno AND bno = :bno LIMIT 1")
    fun getMarkerByMnoBno(mno: Long, bno: Long): Marker? // mno와 bno로 마커 조회
}