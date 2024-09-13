package com.example.liststart.datasource

import com.example.liststart.model.Business
import retrofit2.Response

interface BusinessDataSource {
    suspend fun getBusinessList(): Response<List<Business>>
    suspend fun addBusiness(business: Business): Response<Business>
    suspend fun deleteBusinesses(ids: List<Long>): Response<Unit>
}