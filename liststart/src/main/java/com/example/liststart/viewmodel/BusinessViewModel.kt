package com.example.liststart.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.liststart.datasource.BusinessDataSource
import com.example.liststart.model.Business
import com.example.liststart.view.TAG
import kotlinx.coroutines.launch

class BusinessViewModel(private val businessDataSource: BusinessDataSource) : ViewModel() {

    // 비즈니스 목록을 저장하는 변수와 LiveData
    private var businessItems: MutableList<Business> = mutableListOf()
    private val _businessList = MutableLiveData<MutableList<Business>>()
    val businessList: LiveData<MutableList<Business>>
        get() = _businessList

    // 에러 메시지 전달을 위한 LiveData
    private val _error = MutableLiveData<String>()
    val error: LiveData<String>
        get() = _error

    // 비즈니스 목록을 서버에서 로드하는 함수
    fun loadBusinessList() {
        viewModelScope.launch {
            try {
                val response = businessDataSource.getBusinessList()
                if (response.isSuccessful) {
                    businessItems = response.body()!!.toMutableList()
                    _businessList.value = businessItems
                } else {
                    _error.value = "데이터를 가져오는 데 실패했습니다: ${response.message()}"
                }
            } catch (e: Exception) {
                _error.value = "데이터를 가져오는 중 오류가 발생했습니다: ${e.localizedMessage}"
            }
        }
    }

    // 새로운 비즈니스를 추가하는 함수
    fun addBusiness(business: Business) {
        viewModelScope.launch {
            try {
                val response = businessDataSource.addBusiness(business)
                if (response.isSuccessful) {
                    businessItems.add(0, response.body()!!)
                    _businessList.value = businessItems // 업데이트된 리스트 반영
                } else {
                    _error.value = "비즈니스를 추가하는 데 실패했습니다: ${response.message()}"
                }
            } catch (e: Exception) {
                _error.value = "비즈니스 추가 중 오류가 발생했습니다: ${e.localizedMessage}"
            }
        }
    }

    // 선택된 비즈니스를 삭제하는 함수
    fun deleteSelectedBusinesses(ids: List<Long>) {
        viewModelScope.launch {
            try {
                val response = businessDataSource.deleteBusinesses(ids)
                if (response.isSuccessful) {
                    businessItems.removeAll { it.bno in ids }
                    _businessList.value = businessItems // 업데이트된 리스트 반영
                } else {
                    _error.value = "비즈니스를 삭제하는 데 실패했습니다: ${response.message()}"
                }
            } catch (e: Exception) {
                _error.value = "비즈니스 삭제 중 오류가 발생했습니다: ${e.localizedMessage}"
            }
        }
    }

    // 비즈니스를 수정하는 함수
    fun updateBusiness(business: Business) {
        viewModelScope.launch {
            try {
                val response = businessDataSource.updateBusiness(business.bno!!, business)
                if (response.isSuccessful) {
                    // 서버에서 반환된 업데이트된 비즈니스 객체를 가져옴
                    val updatedBusiness = response.body()

                    // businessItems에서 bno 값이 일치하는 아이템을 찾아 업데이트
                    updatedBusiness?.let { updatedItem ->
                        val index = businessItems.indexOfFirst { it.bno == updatedItem.bno }
                        if (index != -1) {
                            businessItems[index] = updatedItem
                        }

                        // 업데이트된 리스트를 반영
                        _businessList.value = businessItems
                    }
                } else {
                    _error.value = "비즈니스를 수정하는 데 실패했습니다: ${response.message()}"
                }
            } catch (e: Exception) {
                _error.value = "비즈니스 수정 중 오류가 발생했습니다: ${e.localizedMessage}"
            }
        }
    }

    // 비즈니스 리스트 필터링 처리
    fun filterBusiness(query: String) {
        _businessList.value = if (query.isBlank()) {
            businessItems // 검색어가 없으면 전체 리스트 반환
        } else {
            businessItems.filter { it.title.contains(query, ignoreCase = true) }.toMutableList()
        }
    }
}
