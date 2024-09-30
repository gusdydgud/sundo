package com.example.liststart.repository

//import BusinessDao
import com.example.liststart.datasource.BusinessDao

import com.example.liststart.model.Business

class BusinessRepository(private val businessDao: BusinessDao) {

    // 모든 사업체를 가져오는 함수
    suspend fun getAllBusinesses(): List<Business> {
        return businessDao.getBusinessList()
    }

    // 사업체 추가 함수
    suspend fun insertBusiness(business: Business): Long {
        return businessDao.addBusiness(business)  // 삽입된 bno 반환
    }

    // 여러 사업체 삭제 함수 (하나 이상의 사업체 삭제를 처리)
    suspend fun deleteBusinesses(businesses: List<Business>) {
        businessDao.deleteBusinesses(businesses)
    }

    // 사업체 업데이트 함수
    suspend fun updateBusiness(business: Business) {
        businessDao.updateBusiness(business)
    }
}