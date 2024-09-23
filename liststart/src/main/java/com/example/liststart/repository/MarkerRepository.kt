package com.example.liststart.repository

import com.example.liststart.datasource.MarkerDao
import com.example.liststart.model.Marker
import androidx.lifecycle.LiveData

class MarkerRepository(private val markerDao: MarkerDao) {

    // 특정 사업체에 대한 마커 목록을 가져오는 함수
    fun getMarkersForBusiness(bno: Long): LiveData<List<Marker>> {
        return markerDao.getMarkersForBusiness(bno)
    }

    // 로컬 DB에서 특정 사업체에 대한 마커 목록을 가져오는 함수
    fun getMarkersForBusinessFromDb(bno: Long): LiveData<List<Marker>> {
        return markerDao.getMarkersForBusiness(bno)
    }

    // 마커 추가 또는 업데이트 함수
    suspend fun addMarker(marker: Marker): Long {
        return markerDao.addMarker(marker)
    }

    // 마커 삭제 함수
    suspend fun deleteMarkers(markers: List<Marker>): Int {
        return markerDao.deleteMarkers(markers)
    }

    // 마커 업데이트 함수
    suspend fun updateMarker(marker: Marker): Int {
        return markerDao.updateMarker(marker)
    }
}