package com.example.liststart.datasource

import com.example.liststart.entity.BusinessDao
import com.example.liststart.entity.BusinessEntity
import com.example.liststart.model.Business

class BusinessRoomDataSourceImpl(private val businessDao: BusinessDao) : BusinessRoomDataSource {

    override suspend fun getBusinessList(): List<BusinessEntity> {
        return businessDao.getAllBusinesses()
    }

    override suspend fun addBusiness(business: BusinessEntity): BusinessEntity {
        // 데이터 삽입 후, 반환된 ID로 다시 조회
        val id = businessDao.insertBusiness(business)
        return businessDao.getBusinessById(id) ?: throw Exception("Failed to retrieve business after insert")
    }

    override suspend fun deleteBusinesses(ids: List<Long>) {
        businessDao.deleteBusinessesByIds(ids)
    }

    override suspend fun updateBusiness(bno: Long, business: BusinessEntity): BusinessEntity {
        // 업데이트 후, 해당 ID로 다시 조회
        businessDao.updateBusiness(business)
        return businessDao.getBusinessById(bno) ?: throw Exception("Failed to retrieve business after update")
    }
}
