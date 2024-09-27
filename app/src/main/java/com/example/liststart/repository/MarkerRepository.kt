package com.example.liststart.repository

import com.example.liststart.model.Marker

interface MarkerRepository {
    suspend fun getMarkerList(bno: Long): List<Marker>
    suspend fun addMarker(marker: Marker): Marker
    suspend fun deleteMarker(mno: Long)
    suspend fun updateMarker(mno: Long, marker: Marker): Marker
}