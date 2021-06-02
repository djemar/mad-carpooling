package com.mad.carpooling.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mad.carpooling.repository.TripRepository
import com.mad.carpooling.ui.SharedViewModel

class SharedViewModelFactory(private val repo: TripRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SharedViewModel::class.java)) {
            return SharedViewModel(repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }

}