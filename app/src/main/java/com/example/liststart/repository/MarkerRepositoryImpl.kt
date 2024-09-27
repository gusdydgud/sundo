package com.example.liststart.repository

import android.content.Context
import android.util.Log
import com.example.liststart.datasource.MarkerRoomDataSource
import com.example.liststart.entity.toBusinessEntity
import com.example.liststart.entity.toMarker
import com.example.liststart.entity.toMarkerEntity
import com.example.liststart.model.Business
import com.example.liststart.model.Marker

class MarkerRepositoryImpl(
    private val localDataSource: MarkerRoomDataSource,
    private val context: Context
) : MarkerRepository {

    override suspend fun getMarkerList(bno: Long): List<Marker> {
        return try {
            Log.d("MarkerRepository", "Fetching data from local database")
            // 로컬 데이터베이스에서 데이터를 가져옴
            val localData = localDataSource.getMarkerList(bno).map { it.toMarker() }
            if (localData.isNotEmpty()) {
                localData
            } else {
                throw Exception("No data available locally")
            }
        } catch (e: Exception) {
            Log.e("MarkerRepository", "Error getting marker list: ${e.localizedMessage}")
            throw Exception("Error getting marker list: ${e.localizedMessage}")
        }
    }

    override suspend fun addMarker(marker: Marker): Marker {
        return try {
            Log.d("MarkerRepository", "Adding marker to local database")
            // 로컬 데이터베이스에 비즈니스 추가
            localDataSource.addMarker(marker.toMarkerEntity())
            marker
        } catch (e: Exception) {
            Log.e("MarkerRepository", "Error adding marker: ${e.localizedMessage}")
            throw Exception("Error adding marker: ${e.localizedMessage}")
        }
    }

    override suspend fun deleteMarker(mno: Long) {
        try {
            Log.d("MarkerRepository", "Deleting marker from local database")
            // 로컬 데이터베이스에서 비즈니스 삭제
            localDataSource.deleteMarker(mno)
        } catch (e: Exception) {
            Log.e("MarkerRepository", "Error deleting marker: ${e.localizedMessage}")
            throw Exception("Error deleting marker: ${e.localizedMessage}")
        }
    }

    override suspend fun updateMarker(mno: Long, marker: Marker): Marker {
        return try {
            Log.d("MarkerRepository", "Updating Marker in local database")
            // 로컬 데이터베이스에서 비즈니스 업데이트
            localDataSource.updateMarker(mno, marker.toMarkerEntity())
            marker
        } catch (e: Exception) {
            Log.e("MarkerRepository", "Error updating Marker: ${e.localizedMessage}")
            throw Exception("Error updating Marker: ${e.localizedMessage}")
        }
    }
}
