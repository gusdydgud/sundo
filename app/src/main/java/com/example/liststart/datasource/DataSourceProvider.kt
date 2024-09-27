package com.example.liststart.datasource

import android.content.Context
import com.example.liststart.util.Constants
import com.example.liststart.repository.BusinessRepositoryImpl
import com.example.liststart.viewmodel.BusinessViewModelFactory
import com.example.liststart.viewmodel.MarkerViewModelFactory
import com.example.liststart.entity.BusinessDatabase
import com.example.liststart.entity.MarkerDatabase
import com.example.liststart.repository.MarkerRepositoryImpl

object DataSourceProvider {
    // Room DB 인스턴스
    private lateinit var businessRoomDataSource: BusinessRoomDataSource

    // Remote DataSource 인스턴스
    private val businessRemoteDataSource: BusinessDataSource by lazy {
        BusinessDataSourceImpl(Constants.turbineApiService)
    }

    // BusinessRepository 인스턴스
    private lateinit var businessRepository: BusinessRepositoryImpl

    // BusinessViewModelFactory
    lateinit var businessViewModelFactory: BusinessViewModelFactory
        private set



    private lateinit var markerRoomDataSource: MarkerRoomDataSource

    // Marker 관련 싱글톤 인스턴스
    private val markerDataSource: MarkerDataSource by lazy {
        MarkerDataSourceImpl(Constants.turbineApiService)
    }

    // MarkerRepository 인스턴스
    private lateinit var markerRepository: MarkerRepositoryImpl

    val markerViewModelFactory: MarkerViewModelFactory by lazy {
        MarkerViewModelFactory(markerRepository)
    }

    // 초기화 함수 (애플리케이션 시작 시 호출)
    fun init(context: Context) {
        val db = BusinessDatabase.getInstance(context) // Room Database 초기화
        businessRoomDataSource = BusinessRoomDataSourceImpl(db.businessDao())

        // BusinessRepository 및 ViewModelFactory 초기화 (로컬 데이터베이스만 사용)
        businessRepository = BusinessRepositoryImpl(localDataSource = businessRoomDataSource, context = context)
        businessViewModelFactory = BusinessViewModelFactory(businessRepository)

        val dbMarker = MarkerDatabase.getInstance(context) // Marker Room Database 초기화
        markerRoomDataSource = MarkerRoomDataSourceImpl(dbMarker.markerDao())

        // MarkerRepository 및 ViewModelFactory 초기화 (로컬 데이터베이스만 사용)
        markerRepository = MarkerRepositoryImpl(localDataSource = markerRoomDataSource, context)
    }
}
