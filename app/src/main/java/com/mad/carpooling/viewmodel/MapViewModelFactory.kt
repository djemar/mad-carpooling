package com.mad.carpooling.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mad.carpooling.repository.MapRepository

class MapViewModelFactory(private val repo: MapRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MapViewModel::class.java)) {
            return MapViewModel(repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }

}