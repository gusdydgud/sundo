package com.example.liststart.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.liststart.model.Marker
import com.example.liststart.repository.MarkerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    // 로컬 DB에서 특정 사업(bno)에 대한 마커 목록 로드
    fun loadMarkerListFromDb(bno: Long) {
        viewModelScope.launch {
            try {
                markerRepository.getMarkersForBusinessFromDb(bno).observeForever { markers ->
                    Log.d("MarkerViewModel", "Loaded markers from DB for bno: $bno, marker count: ${markers.size}")
                    markers.forEach { marker ->
                        Log.d("MarkerViewModel", "Marker details: mno = ${marker.mno}, latitude = ${marker.latitude}, longitude = ${marker.longitude}")
                    }
                    _markerList.value = markers.toMutableList()  // LiveData 값을 수신하고 업데이트
                }
            } catch (e: Exception) {
                Log.e("MarkerViewModel", "Error loading markers from DB: ${e.localizedMessage}")
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

    // mno와 bno를 받아서 Marker 객체를 조회한 후 삭제하는 함수
    fun deleteMarker(mno: Long, bno: Long) {
        viewModelScope.launch {
            try {
                // 백그라운드 스레드에서 DB 작업 수행
                withContext(Dispatchers.IO) {
                    Log.d("MarkerViewModel", "Attempting to delete marker with mno: $mno and bno: $bno")

                    val marker = markerRepository.getMarkerByMnoBno(mno, bno) // mno와 bno로 마커 객체 조회
                    marker?.let {
                        val deletedCount = markerRepository.deleteMarkers(listOf(it))
                        if (deletedCount > 0) {
                            // 마커가 삭제되었을 때, UI 업데이트는 메인 스레드에서 처리
                            withContext(Dispatchers.Main) {
                                val updatedList = markerList.value?.toMutableList() ?: mutableListOf()
                                updatedList.remove(it)
                                _markerList.postValue(updatedList) // LiveData 업데이트
                                Log.d("MarkerViewModel", "Marker deleted successfully: mno = ${it.mno}, updated marker list size: ${updatedList.size}")
                            }
                        } else {
                            // 삭제 실패 시 에러 메시지
                            withContext(Dispatchers.Main) {
                                Log.e("MarkerViewModel", "Failed to delete marker: mno = ${it.mno}")
                                _error.postValue("마커 삭제 실패")
                            }
                        }
                    } ?: run {
                        // 마커를 찾을 수 없는 경우 에러 처리
                        withContext(Dispatchers.Main) {
                            Log.e("MarkerViewModel", "Marker not found: mno = $mno, bno = $bno")
                            _error.postValue("마커를 찾을 수 없음")
                        }
                    }
                }
            } catch (e: Exception) {
                // 예외 처리
                Log.e("MarkerViewModel", "Error while deleting marker: ${e.localizedMessage}", e)
                withContext(Dispatchers.Main) {
                    _error.postValue("오류 발생: ${e.localizedMessage}")
                }
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
