package com.example.liststart.datasource

import com.example.liststart.model.Business
import com.example.liststart.service.TurbineAPIService
import retrofit2.Response

class BusinessDataSourceImpl(private val apiService: TurbineAPIService) : BusinessDataSource {

    override suspend fun getBusinessList(): Response<List<Business>> {
        return apiService.getBusinessAll()
    }

    override suspend fun addBusiness(business: Business): Response<Business> {
        return apiService.addBusiness(business)
    }

    override suspend fun deleteBusinesses(ids: List<Long>): Response<Unit> {
        return apiService.deleteBusinesses(ids)
    }
}