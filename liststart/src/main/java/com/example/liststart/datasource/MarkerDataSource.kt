package com.example.liststart.datasource

import com.example.liststart.model.Business
import com.example.liststart.model.Marker
import retrofit2.Response

interface MarkerDataSource {
    suspend fun getMarkerList(bno: Long): Response<List<Marker>>
}