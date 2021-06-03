package com.mad.carpooling.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mad.carpooling.repository.MapRepository
import com.mad.carpooling.repository.UserRepository

class EditProfileViewModelFactory(private val repo: UserRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EditProfileViewModel::class.java)) {
            return EditProfileViewModel(repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }

}