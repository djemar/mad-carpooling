package com.mad.carpooling.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mad.carpooling.repository.TripRepository
import com.mad.carpooling.repository.UserRepository

class SharedViewModelFactory(
    private val tripRepository: TripRepository,
    private val userRepository: UserRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SharedViewModel::class.java)) {
            return SharedViewModel(tripRepository, userRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }

}