package com.example.liststart.datasource

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.liststart.model.Business


@Dao
interface BusinessDao {

    @Query("SELECT * FROM business")
    suspend fun getBusinessList(): List<Business>  // suspend fun으로 비동기 처리

    @Insert(onConflict = OnConflictStrategy.REPLACE) //INSERT
    suspend fun addBusiness(business: Business): Long  // 추가된 행의 ID를 반환

    @Delete
    suspend fun deleteBusinesses(business: List<Business>): Int  // 삭제된 행의 수 반환

    @Update
    suspend fun updateBusiness(business: Business): Int  // 업데이트된 행의 수 반환
}