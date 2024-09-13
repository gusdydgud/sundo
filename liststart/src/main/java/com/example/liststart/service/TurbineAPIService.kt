package com.example.liststart.service

import com.example.liststart.model.Business
import retrofit2.http.GET
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.Response

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
}