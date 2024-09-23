//package com.example.liststart.datasource
//
//import com.example.liststart.model.Business
//import com.example.liststart.model.Marker
//import retrofit2.Response
//
//interface MarkerDataSource {
//    suspend fun getMarkerList(bno: Long): Response<List<Marker>>
//    suspend fun addMarker(marker: Marker): Response<Marker>
//    suspend fun updateMarker(mno: Long, marker: Marker): Response<Marker>
//    suspend fun deleteMarker(mno: Long): Response<Unit> // 삭제 함수 추가
//}