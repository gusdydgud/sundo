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
    @GET("business/all")
    suspend fun getBusinessAll(): Response<List<Business>>

    @Headers("Content-Type: application/json")
    @POST("business/add")
    suspend fun addBusiness(@Body business: Business): Response<Business>

    @Headers("Content-Type: application/json")
    @POST("business/delete")
    suspend fun deleteBusinesses(@Body ids: List<Long>): Response<Unit>

    @Headers("Content-Type: application/json")
    @POST("business/update/{bno}")
    suspend fun updateBusiness(@Path(value = "bno") bno: Long, @Body business: Business): Response<Business>

    @Headers("Content-Type: application/json")
    @GET("marker/{bno}")
    suspend fun getMarkerList(@Path(value = "bno") bno: Long): Response<List<Marker>>

    @Headers("Content-Type: application/json")
    @POST("pmarker/add")
    suspend fun addMarker(@Body marker: Marker): Response<Marker>

    @Headers("Content-Type: application/json")
    @POST("marker/update/{mno}")
    suspend fun updateMarker(@Path(value = "mno") mno: Long, @Body marker: Marker): Response<Marker>

    @Headers("Content-Type: application/json")
    @DELETE("marker/delete/{mno}")
    suspend fun deleteMarker(@Path("mno") mno: Long): Response<Unit>

}