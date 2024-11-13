package com.example.liststart.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.liststart.datasource.MarkerDataSource
import com.example.liststart.model.Marker
import kotlinx.coroutines.launch

class MarkerViewModel(private val markerDataSource: MarkerDataSource) : ViewModel() {

    // Marker List
    private var markerItems: MutableList<Marker> = mutableListOf()
    private val _markerList = MutableLiveData<MutableList<Marker>>()
    val markerList: LiveData<MutableList<Marker>>
        get() = _markerList

    // Error handling
    private val _error = MutableLiveData<String>()
    val error: LiveData<String>
        get() = _error

    // 마커 업데이트 결과를 전달할 LiveData
    private val _updateResult = MutableLiveData<String>()
    val updateResult: LiveData<String>
        get() = _updateResult

    // 특정 사업(bno)에 대한 마커 목록 로드
    fun loadMarkerList(bno: Long) {
        viewModelScope.launch {
            try {
                val response = markerDataSource.getMarkerList(bno)
                if (response.isSuccessful && response.body() != null) {
                    markerItems = response.body()!!.toMutableList()
                    _markerList.postValue(markerItems)
                } else {
                    _error.postValue("마커 목록을 불러오지 못했습니다: ${response.message()}")
                }
            } catch (e: Exception) {
                _error.postValue("마커 목록을 불러오는 중 오류 발생: ${e.localizedMessage}")
            }
        }
    }

    // 마커 추가 시 로그 출력
    fun addMarker(marker: Marker, onSuccess: (Marker) -> Unit, onFailure: (String) -> Unit) {
        viewModelScope.launch {
            try {
                Log.d("MarkerLog", "마커 추가 요청 시작")
                Log.d("MarkerLog", "전송할 마커 데이터: $marker")

                val response = markerDataSource.addMarker(marker)
                val newMarker = response.body()

                if (response.isSuccessful && newMarker != null) {
                    markerItems.add(0, newMarker)
                    _markerList.postValue(markerItems.toMutableList())
                    Log.d("MarkerLog", "마커 추가 성공: $newMarker")
                    onSuccess(newMarker)
                } else {
                    val errorCode = response.code()
                    val errorMessage = response.message()
                    val errorBody = response.errorBody()?.string() // 에러 본문 추가로 출력

                    Log.e("MarkerLog", "마커 추가 실패: $errorMessage (상태 코드: $errorCode, 에러 본문: $errorBody)")
                    onFailure("마커 추가 실패: $errorMessage (상태 코드: $errorCode, 에러 본문: $errorBody)")
                }
            } catch (e: Exception) {
                Log.e("MarkerLog", "마커 추가 중 오류 발생: ${e.localizedMessage}")
                onFailure("마커 추가 중 오류 발생: ${e.localizedMessage}")
            }
        }
    }



    // mno로 마커 삭제
    fun deleteMarker(mno: Long, bno: Long) {
        viewModelScope.launch {
            try {
                val response = markerDataSource.deleteMarker(mno)
                if (response.isSuccessful) {
                    // 마커 리스트에서 해당 마커를 제거
                    markerItems.removeAll { it.mno == mno }
                    _markerList.postValue(markerItems.toMutableList()) // LiveData 업데이트
                } else {
                    _error.postValue("마커 삭제 실패: ${response.message()}")
                }
            } catch (e: Exception) {
                _error.postValue("오류 발생: ${e.localizedMessage}")
            }
        }
    }

    // 마커 업데이트
    fun updateMarker(marker: Marker) {
        val mno = marker.mno
        if (mno == null) {
            _error.value = "마커 ID가 없습니다."
            return
        }

        viewModelScope.launch {
            try {
                val response = markerDataSource.updateMarker(marker.mno, marker) // mno 경로 변수로 추가
                if (response.isSuccessful) {
                    _updateResult.postValue("마커가 서버에 업데이트되었습니다.")
                    // 업데이트 성공 시, 기존 마커 목록을 다시 로드하여 UI 갱신
                    loadMarkerList(marker.bno)
                } else {
                    _updateResult.postValue("마커 업데이트 실패: ${response.message()}")
                }
            } catch (e: Exception) {
                _updateResult.postValue("오류 발생: ${e.message}")
            }
        }
    }
}
