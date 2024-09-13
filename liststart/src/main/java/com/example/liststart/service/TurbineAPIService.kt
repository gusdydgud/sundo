package com.example.liststart.service

import com.example.liststart.Item
import retrofit2.http.GET
import retrofit2.Call

interface TurbineAPIService {

    @GET("get/business/all")
    fun getBusinessAll() : Call<ArrayList<Item>>
}