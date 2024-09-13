package com.example.myapplication.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object Constants { // 싱글톤

    const val API_KEY = "7L7mEFjUTa3dkEGmdCVmIaIu8Kt30GlD4wPphu02"
    const val BASE_URL = "https://j8kft7iw86.execute-api.ap-northeast-2.amazonaws.com/default/"

    // 데이터 주고받을때 언더바를 카멜표기법으로 바꿔주는 작업
    val gson = GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create()
    // Retrofit 연결
    fun createRetrofit(): APIService {

        // Retrofit 연결 객체
        val retrofit: Retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson)) // GSON 으로 컨버팅
            .build()

        // 사용할 인터페이스
        val apiService = retrofit.create(APIService::class.java)
        return apiService
    }

    // 인터넷 연결상태 확인
    fun isNetworkAvailable(context: Context): Boolean {

        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // API 버전에 따라 다른 코드 작성
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

            // 사용가능한 네트워크가 없다면 false
            val network = connectivityManager.activeNetwork ?: return false
            // 사용가능한 인터넷, 와이파이 연결 여부 확인 없다면 false
            val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false

            return when {
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> return true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> return true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> return true
                else -> false
            }

        } else {
            val networkInfo = connectivityManager.activeNetworkInfo
            return networkInfo != null && networkInfo.isConnectedOrConnecting
        }


    }

}