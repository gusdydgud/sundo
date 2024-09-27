package com.example.liststart.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.liststart.datasource.MarkerDataSource
import com.example.liststart.model.Marker
import com.example.liststart.repository.MarkerRepository
import kotlinx.coroutines.launch

class MarkerViewModel(private val markerRepository: MarkerRepository) : ViewModel() {

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
                val markers = markerRepository.getMarkerList(bno)
                markerItems = markers.toMutableList()
                _markerList.postValue(markerItems)
            } catch (e: Exception) {
                _error.postValue("마커 목록을 불러오는 중 오류 발생: ${e.localizedMessage}")
            }
        }
    }

    // 새 마커 추가
    fun addMarker(marker: Marker, onSuccess: (Marker) -> Unit, onFailure: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val newMarker = markerRepository.addMarker(marker)
                markerItems.add(0, newMarker)
                _markerList.postValue(markerItems.toMutableList()) // 목록 업데이트
                onSuccess(newMarker) // UI에 알림
            } catch (e: Exception) {
                onFailure("마커 추가 중 오류 발생: ${e.localizedMessage}")
            }
        }
    }

    // mno로 마커 삭제
    fun deleteMarker(mno: Long, bno: Long) {
        viewModelScope.launch {
            try {
                markerRepository.deleteMarker(mno)
                markerItems.removeAll { it.mno == mno }
                _markerList.postValue(markerItems.toMutableList()) // LiveData 업데이트
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
                val marker = markerRepository.updateMarker(marker.mno, marker) // mno 경로 변수로 추가
                _updateResult.postValue("마커가 서버에 업데이트되었습니다.")
                // 업데이트 성공 시, 기존 마커 목록을 다시 로드하여 UI 갱신
                loadMarkerList(marker.bno)
            } catch (e: Exception) {
                _updateResult.postValue("오류 발생: ${e.message}")
            }
        }
    }
}
