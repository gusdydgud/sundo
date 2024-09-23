package com.example.liststart.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.liststart.model.Marker
import com.example.liststart.repository.MarkerRepository
import kotlinx.coroutines.launch

class MarkerViewModel(private val markerRepository: MarkerRepository) : ViewModel() {

    // Marker List
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
//    fun loadMarkerList(bno: Long) {
//        viewModelScope.launch {
//            try {
//                markerRepository.getMarkersForBusiness(bno).observeForever { markers ->
//                    _markerList.value = markers.toMutableList()  // LiveData 값을 수신하고 업데이트
//                }
//            } catch (e: Exception) {
//                _error.postValue("마커 목록을 불러오는 중 오류 발생: ${e.localizedMessage}")
//            }
//        }
//    }
    // 로컬 DB에서 특정 사업(bno)에 대한 마커 목록 로드
    fun loadMarkerListFromDb(bno: Long) {
        viewModelScope.launch {
            try {
                markerRepository.getMarkersForBusinessFromDb(bno).observeForever { markers ->
                    _markerList.value = markers.toMutableList()  // LiveData 값을 수신하고 업데이트
                }
            } catch (e: Exception) {
                _error.postValue("로컬 DB에서 마커 목록을 불러오는 중 오류 발생: ${e.localizedMessage}")
            }
        }
    }

    // 새 마커 추가
    fun addMarker(marker: Marker, onSuccess: (Marker) -> Unit, onFailure: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val newMarkerId = markerRepository.addMarker(marker)
                if (newMarkerId > 0) {
                    val newMarker = marker.copy(mno = newMarkerId)  // 새로 반환된 ID를 사용한 복사본 생성
                    val updatedList = markerList.value?.toMutableList() ?: mutableListOf()
                    updatedList.add(0, newMarker)
                    _markerList.postValue(updatedList) // 목록 업데이트
                    onSuccess(newMarker) // UI에 알림
                } else {
                    onFailure("마커 추가 실패")
                }
            } catch (e: Exception) {
                onFailure("마커 추가 중 오류 발생: ${e.localizedMessage}")
            }
        }
    }

    // mno로 마커 삭제
    fun deleteMarker(marker: Marker) {
        viewModelScope.launch {
            try {
                val deletedCount = markerRepository.deleteMarkers(listOf(marker))
                if (deletedCount > 0) {
                    val updatedList = markerList.value?.toMutableList() ?: mutableListOf() // null일 경우 빈 리스트로 초기화
                    updatedList.remove(marker)
                    _markerList.postValue(updatedList) // LiveData 업데이트
                } else {
                    _error.postValue("마커 삭제 실패")
                }
            } catch (e: Exception) {
                _error.postValue("오류 발생: ${e.localizedMessage}")
            }
        }
    }

    // 마커 업데이트
    fun updateMarker(marker: Marker) {
        viewModelScope.launch {
            try {
                val updatedRows = markerRepository.updateMarker(marker)
                if (updatedRows > 0) {
                    _updateResult.postValue("마커가 로컬 DB에 업데이트되었습니다.")
                    // 업데이트 성공 시, 기존 마커 목록을 다시 로드하여 UI 갱신
                    loadMarkerListFromDb(marker.bno) // 수정된 부분
                } else {
                    _updateResult.postValue("마커 업데이트 실패")
                }
            } catch (e: Exception) {
                _updateResult.postValue("오류 발생: ${e.message}")
            }
        }
    }
}