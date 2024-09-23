package com.example.liststart.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.liststart.model.Business
import com.example.liststart.repository.BusinessRepository
import kotlinx.coroutines.launch

class BusinessViewModel(private val businessRepository: BusinessRepository) : ViewModel() {

    // Business List
    private var businessItems: MutableList<Business> = mutableListOf()
    private val _businessList = MutableLiveData<MutableList<Business>>()
    val businessList: LiveData<MutableList<Business>>
        get() = _businessList

    // CheckBox Visibility
    private val _isCheckBoxVisible = MutableLiveData<Boolean>()
    val isCheckBoxVisible: LiveData<Boolean>
        get() = _isCheckBoxVisible

    // 에러 메시지 전달을 위한 LiveData
    private val _error = MutableLiveData<String>()
    val error: LiveData<String>
        get() = _error

    // 비즈니스 목록을 로컬 DB에서 로드하는 함수
    fun loadBusinessList() {
        viewModelScope.launch {
            try {
                val businesses = businessRepository.getAllBusinesses() // DB에서 데이터 가져오기
                businessItems = businesses.toMutableList()
                _businessList.value = businessItems
            } catch (e: Exception) {
                _error.value = "데이터를 가져오는 중 오류가 발생했습니다: ${e.localizedMessage}"
            }
        }
    }

    // 새로운 비즈니스를 추가하는 함수
    fun addBusiness(business: Business) {
        viewModelScope.launch {
            try {
                businessRepository.insertBusiness(business) // DB에 데이터 추가
                businessItems.add(0, business)
                _businessList.value = businessItems.toMutableList() // 업데이트된 리스트 반영
            } catch (e: Exception) {
                _error.value = "비즈니스 추가 중 오류가 발생했습니다: ${e.localizedMessage}"
            }
        }
    }

    // 선택된 비즈니스를 삭제하는 함수
    fun deleteSelectedBusinesses(ids: List<Long>) {
        viewModelScope.launch {
            try {
                val businessesToDelete = businessItems.filter { it.bno in ids }
                businessRepository.deleteBusinesses(businessesToDelete) // `List<Business>`로 삭제
                businessItems.removeAll { it.bno in ids }
                _businessList.value = businessItems.toMutableList() // 업데이트된 리스트 반영
            } catch (e: Exception) {
                _error.value = "비즈니스 삭제 중 오류가 발생했습니다: ${e.localizedMessage}"
            }
        }
    }

    // 비즈니스를 수정하는 함수
    fun updateBusiness(business: Business) {
        val bno = business.bno
        if (bno == null) { // null 처리
            _error.value = "수정할 비즈니스의 ID가 없습니다."
            return
        }

        viewModelScope.launch {
            try {
                businessRepository.updateBusiness(business) // DB에서 데이터 수정
                // businessItems에서 bno 값이 일치하는 아이템을 찾아 업데이트
                val index = businessItems.indexOfFirst { it.bno == bno }
                if (index != -1) {
                    businessItems[index] = business
                }

                // 업데이트된 리스트를 반영
                _businessList.value = businessItems.toMutableList()
            } catch (e: Exception) {
                _error.value = "비즈니스 수정 중 오류가 발생했습니다: ${e.localizedMessage}"
            }
        }
    }

    // 비즈니스 리스트 필터링 처리
    fun filterBusiness(query: String) {
        _businessList.value = if (query.isBlank()) {
            businessItems.toMutableList() // 검색어가 없으면 전체 리스트 반환
        } else {
            businessItems.filter { it.title.contains(query, ignoreCase = true) }.toMutableList()
        }
    }

    // 현재 선택되어 넘어간 비즈니스를 제외하여 화면에 띄우기
    fun loadBusinessListFromDbExcluding(excludeBusiness: Business) {
        viewModelScope.launch {
            try {
                val businesses = businessRepository.getAllBusinesses() // DB에서 데이터 가져오기
                // excludeBusiness와 bno가 다른 항목만 필터링
                businessItems = businesses.filter { it.bno != excludeBusiness.bno }.toMutableList()
                _businessList.value = businessItems
            } catch (e: Exception) {
                _error.value = "데이터를 가져오는 중 오류가 발생했습니다: ${e.localizedMessage}"
            }
        }
    }

    // 체크박스 클릭 처리
    fun toggleBusinessItemCheck(business: Business) {
        business.isChecked = !business.isChecked
        _businessList.value = businessItems.toMutableList()
    }

    // 체크박스 가시성 토글
    fun toggleCheckBoxVisibility() {
        _isCheckBoxVisible.value = !(_isCheckBoxVisible.value ?: false)
    }

    fun isAnyChecked(): Boolean {
        return businessList.value?.any { it.isChecked } ?: false
    }

    fun clearAllChecks() {
        businessList.value?.forEach { it.isChecked = false }
        _businessList.value = businessList.value // 업데이트된 리스트 반영
    }

    fun getSelectedBusinessIds(): List<Long> {
        return businessList.value?.filter { it.isChecked }?.mapNotNull { it.bno } ?: emptyList()
    }

    fun getCheckedItemTitle(): String {
        val checkedItems = businessList.value?.filter { it.isChecked }?.map { it.title } ?: emptyList()
        return if (checkedItems.size > 3) {
            "선택된 사업 ${checkedItems.size}개"
        } else {
            checkedItems.joinToString("\n")
        }
    }
}