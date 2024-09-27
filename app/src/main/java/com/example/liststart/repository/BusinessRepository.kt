package com.example.liststart.repository

import com.example.liststart.model.Business

interface BusinessRepository {
    suspend fun getBusinessList(): List<Business>
    suspend fun addBusiness(business: Business): Business
    suspend fun deleteBusinesses(ids: List<Long>)
    suspend fun updateBusiness(bno: Long, business: Business): Business
}