//package com.example.liststart.datasource
//
//import com.example.liststart.model.Marker
//import com.example.liststart.service.TurbineAPIService
//import retrofit2.Response
//
//class MarkerDataSourceImpl(private val apiService: TurbineAPIService): MarkerDataSource {
//
//    override suspend fun getMarkerList(bno: Long): Response<List<Marker>> {
//        return apiService.getMarkerList(bno)
//    }
//
//    override suspend fun addMarker(marker: Marker): Response<Marker> {
//        return apiService.addMarker(marker)
//    }
//
//    override suspend fun updateMarker(mno: Long, marker: Marker): Response<Marker> {
//        return apiService.updateMarker(mno, marker)
//    }
//    override suspend fun deleteMarker(mno: Long): Response<Unit> {
//        return apiService.deleteMarker(mno) // 마커 삭제 API 호출
//    }
//}