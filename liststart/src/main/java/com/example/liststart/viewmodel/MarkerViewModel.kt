package com.example.liststart.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.liststart.datasource.MarkerDataSource
import com.example.liststart.model.Marker
import kotlinx.coroutines.launch

class MarkerViewModel(private val markerDataSource: MarkerDataSource) : ViewModel() {

    // 마커 목록 저장하는 변수와 LiveData
    private var markerItems: MutableList<Marker> = mutableListOf()
    private val _markerList = MutableLiveData<MutableList<Marker>>()
    val markerList: LiveData<MutableList<Marker>>
        get() = _markerList

    // 에러
    private val _error = MutableLiveData<String>()
    val error: LiveData<String>
        get() = _error

    // 삭제 기능 수정: 삭제 후 UI에서 바로 제거
    fun deleteMarker(mno: Long) {
        viewModelScope.launch {
            try {
                val response = markerDataSource.deleteMarker(mno)
                if (response.isSuccessful) {
                    // 마커 리스트에서 해당 마커를 제거
                    markerItems.removeAll { it.mno == mno }
                    _markerList.value = markerItems // UI 업데이트

                } else {
                    _error.value = "마커 삭제 실패: ${response.message()}"
                }
            } catch (e: Exception) {
                _error.value = "오류 발생: ${e.message}"
            }
        }
    }

    // 마커 목록을 서버에서 로드
    fun loadMarkerList(bno: Long) {
        viewModelScope.launch {
            try {
                val response = markerDataSource.getMarkerList(bno)
                if (response.isSuccessful) {
                    markerItems = response.body()!!.toMutableList()
                    _markerList.value = markerItems
                } else {
                    _error.value = "데이터를 가져오는 데 실패했습니다: ${response.message()}"
                }
            } catch (e: Exception) {
                _error.value = "데이터를 가져오는 중 오류가 발생했습니다: ${e.localizedMessage}"
            }
        }
    }
}