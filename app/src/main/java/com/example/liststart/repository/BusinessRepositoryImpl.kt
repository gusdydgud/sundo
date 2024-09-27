package com.example.liststart.repository

import android.content.Context
import android.util.Log
import com.example.liststart.datasource.BusinessRoomDataSource
import com.example.liststart.entity.toBusiness
import com.example.liststart.entity.toBusinessEntity
import com.example.liststart.model.Business

class BusinessRepositoryImpl(
    private val localDataSource: BusinessRoomDataSource,
    private val context: Context
) : BusinessRepository {

    override suspend fun getBusinessList(): List<Business> {
        return try {
            Log.d("BusinessRepository", "Fetching data from local database")
            // 로컬 데이터베이스에서 데이터를 가져옴
            val localData = localDataSource.getBusinessList().map { it.toBusiness() }
            if (localData.isNotEmpty()) {
                localData
            } else {
                throw Exception("No data available locally")
            }
        } catch (e: Exception) {
            Log.e("BusinessRepository", "Error getting business list: ${e.localizedMessage}")
            throw Exception("Error getting business list: ${e.localizedMessage}")
        }
    }

    override suspend fun addBusiness(business: Business): Business {
        return try {
            Log.d("BusinessRepository", "Adding business to local database")
            // 로컬 데이터베이스에 비즈니스 추가
            localDataSource.addBusiness(business.toBusinessEntity())
            business
        } catch (e: Exception) {
            Log.e("BusinessRepository", "Error adding business: ${e.localizedMessage}")
            throw Exception("Error adding business: ${e.localizedMessage}")
        }
    }

    override suspend fun deleteBusinesses(ids: List<Long>) {
        try {
            Log.d("BusinessRepository", "Deleting businesses from local database")
            // 로컬 데이터베이스에서 비즈니스 삭제
            localDataSource.deleteBusinesses(ids)
        } catch (e: Exception) {
            Log.e("BusinessRepository", "Error deleting businesses: ${e.localizedMessage}")
            throw Exception("Error deleting businesses: ${e.localizedMessage}")
        }
    }

    override suspend fun updateBusiness(bno: Long, business: Business): Business {
        return try {
            Log.d("BusinessRepository", "Updating business in local database")
            // 로컬 데이터베이스에서 비즈니스 업데이트
            localDataSource.updateBusiness(bno, business.toBusinessEntity())
            business
        } catch (e: Exception) {
            Log.e("BusinessRepository", "Error updating business: ${e.localizedMessage}")
            throw Exception("Error updating business: ${e.localizedMessage}")
        }
    }
}
