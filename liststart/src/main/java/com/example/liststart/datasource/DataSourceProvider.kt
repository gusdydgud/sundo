package com.example.liststart.datasource


import android.content.Context
import androidx.room.Room
import com.example.liststart.datasource.BusinessDatabase// 데이터베이스 클래스 임포트
import com.example.liststart.repository.BusinessRepository
import com.example.liststart.repository.MarkerRepository
import com.example.liststart.viewmodel.BusinessViewModelFactory
import com.example.liststart.viewmodel.MarkerViewModelFactory

object DataSourceProvider {

    // Room 데이터베이스 참조
    private lateinit var database: BusinessDatabase // 실제 데이터베이스 타입으로 수정

    fun initializeDatabase(context: Context) {
        database = Room.databaseBuilder(
            context.applicationContext,
            BusinessDatabase::class.java, // 실제 데이터베이스 클래스 사용
            "your-database-name" // 데이터베이스 이름
        ).build()
    }

    // BusinessViewModelFactory를 제공
    val businessViewModelFactory: BusinessViewModelFactory
        get() = BusinessViewModelFactory(BusinessRepository(database.businessDao()))

    // MarkerViewModelFactory 제공
    val markerViewModelFactory: MarkerViewModelFactory by lazy {
        MarkerViewModelFactory(MarkerRepository(database.markerDao())) // MarkerDao를 사용하는 MarkerRepository로 변경
    }
}