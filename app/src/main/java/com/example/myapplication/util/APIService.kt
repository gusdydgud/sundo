package com.example.myapplication.util

import retrofit2.Call
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Headers

interface APIService {

    @Headers("x-api-key: ${Constants.API_KEY}")
    @GET("android-getMenu")
    //fun getMenu() : Call<List<Menu>>

    // 코루틴 방식
    suspend fun getMenu(): Response<List<Menu>>
}