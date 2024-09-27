package com.example.liststart.datasource

import com.example.liststart.entity.BusinessEntity
import com.example.liststart.model.Business

interface BusinessRoomDataSource {
    suspend fun getBusinessList(): List<BusinessEntity>
    suspend fun addBusiness(business: BusinessEntity): BusinessEntity
    suspend fun deleteBusinesses(ids: List<Long>): Unit
    suspend fun updateBusiness(bno: Long, business: BusinessEntity): BusinessEntity
}