package com.example.liststart.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.liststart.repository.MarkerRepository

class MarkerViewModelFactory(private val markerRepository: MarkerRepository) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MarkerViewModel::class.java)) {
            return MarkerViewModel(markerRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}