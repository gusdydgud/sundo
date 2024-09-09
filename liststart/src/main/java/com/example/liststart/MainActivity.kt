package com.example.liststart

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

const val TAG = "myLog"

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 리사이클러뷰 설정
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this) 

        // 예시 데이터 생성 및 어댑터 설정
        val exampleList = listOf(
            Item("가산 풍력디지털 단지", "2024.09.05", R.drawable.profile, 37.479180, 126.874852),
            Item("송파구 법조타운", "2024.09.01", R.drawable.profile, 37.483817, 127.112121),
            Item("영등포 스마트 도시", "2024.08.30", R.drawable.profile, 37.529931, 126.887700),
            Item("선도 디지털 단지", "2024.08.30", R.drawable.profile, 37.480417, 126.874323),
            Item("제주 탐라 풍력 단지", "2024.08.30", R.drawable.profile, 33.499911, 126.449403),
            Item("영흥 풍력 단지", "2024.08.30", R.drawable.profile, 37.239810, 126.446187)
        )

        recyclerView.adapter = ItemAdapter(exampleList, { item -> handleClick(item) } )

        // 검색 버튼 클릭 리스너
        val searchButton = findViewById<ImageButton>(R.id.searchButton)
        searchButton.setOnClickListener {
            val intent = Intent(this, GisActivity::class.java)
            startActivity(intent)
        }

        // EditText 포커스 상태에 따른 밑줄 색상 변경
        val searchEditText = findViewById<EditText>(R.id.searchEditText)
        searchEditText.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                // 포커스가 활성화되면 밑줄 색상 변경
                searchEditText.backgroundTintList = ContextCompat.getColorStateList(this, R.color.black)
            } else {
                // 포커스가 비활성화되면 밑줄 색상 변경
                searchEditText.backgroundTintList = ContextCompat.getColorStateList(this, R.color.white)
            }
        }
    }

    fun handleClick(data: Item) {
        Log.d(TAG, ": 실행됨 $data")
        val intent = Intent(this, GisActivity::class.java)
        val bundle = Bundle()
        bundle.putString("title", data.title)
        bundle.putDouble("lat", data.lat)
        bundle.putDouble("long", data.long)
        intent.putExtras(bundle)
        startActivity(intent)
    }



}
