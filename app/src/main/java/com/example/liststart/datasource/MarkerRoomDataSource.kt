package com.example.liststart.datasource

import com.example.liststart.entity.MarkerEntity
import com.example.liststart.model.Business
import com.example.liststart.model.Marker
import retrofit2.Response

interface MarkerRoomDataSource {
    suspend fun getMarkerList(bno: Long): List<MarkerEntity>
    suspend fun addMarker(marker: MarkerEntity): MarkerEntity
    suspend fun updateMarker(mno: Long, marker: MarkerEntity): MarkerEntity
    suspend fun deleteMarker(mno: Long): Unit // 삭제 함수 추가
}