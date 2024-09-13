package com.example.liststart.datasource

import com.example.liststart.model.Marker
import com.example.liststart.service.TurbineAPIService
import retrofit2.Response

class MarkerDataSourceImpl(private val apiService: TurbineAPIService): MarkerDataSource {

    override suspend fun getMarkerList(bno: Long): Response<List<Marker>> {
        return apiService.getMarkerList(bno)
    }
}