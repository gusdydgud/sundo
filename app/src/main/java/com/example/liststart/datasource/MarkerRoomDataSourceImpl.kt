package com.example.liststart.datasource

import com.example.liststart.entity.BusinessDao
import com.example.liststart.entity.BusinessEntity
import com.example.liststart.entity.MarkerDao
import com.example.liststart.entity.MarkerEntity
import com.example.liststart.model.Business

class MarkerRoomDataSourceImpl(private val markerDao: MarkerDao) : MarkerRoomDataSource {

    override suspend fun getMarkerList(bno: Long): List<MarkerEntity> {
        return markerDao.getAllMarkersById(bno)
    }

    override suspend fun addMarker(marker: MarkerEntity): MarkerEntity {
        // 데이터 삽입 후, 반환된 ID로 다시 조회
        val id = markerDao.insertMarker(marker)
        return markerDao.getMarkerById(id) ?: throw Exception("Failed to retrieve business after insert")
    }

    override suspend fun deleteMarker(mno: Long): Unit {
        markerDao.deleteMarkerById(mno)
    }

    override suspend fun updateMarker(mno: Long, marker: MarkerEntity): MarkerEntity {
        // 업데이트 후, 해당 ID로 다시 조회
        markerDao.updateMarker(marker)
        return markerDao.getMarkerById(mno) ?: throw Exception("Failed to retrieve business after update")
    }
}
