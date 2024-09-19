package com.example.liststart.service

import com.example.liststart.model.Business
import com.example.liststart.model.Marker
import retrofit2.http.GET
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.Path

interface TurbineAPIService {

    @Headers("Content-Type: application/json")
    @GET("get/business/all")
    suspend fun getBusinessAll(): Response<List<Business>>

    @Headers("Content-Type: application/json")
    @POST("post/business/add")
    suspend fun addBusiness(@Body business: Business): Response<Business>

    @Headers("Content-Type: application/json")
    @POST("delete/business")
    suspend fun deleteBusinesses(@Body ids: List<Long>): Response<Unit>

    @Headers("Content-Type: application/json")
    @POST("post/business/update/{bno}")
    suspend fun updateBusiness(@Path(value = "bno") bno: Long, @Body business: Business): Response<Business>

    @Headers("Content-Type: application/json")
    @GET("get/marker/{bno}")
    suspend fun getMarkerList(@Path(value = "bno") bno: Long): Response<List<Marker>>

    @Headers("Content-Type: application/json")
    @POST("post/marker/add")
    suspend fun addMarker(@Body marker: Marker): Response<Marker>

    @Headers("Content-Type: application/json")
    @POST("post/marker/update/{mno}")
    suspend fun updateMarker(@Path(value = "mno") mno: Long, @Body marker: Marker): Response<Marker>

    @Headers("Content-Type: application/json")
    @DELETE("delete/marker/{mno}")
    suspend fun deleteMarker(@Path("mno") mno: Long): Response<Unit>

}