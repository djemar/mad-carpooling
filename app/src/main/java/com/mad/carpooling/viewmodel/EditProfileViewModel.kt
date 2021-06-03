package com.mad.carpooling.viewmodel

import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.mad.carpooling.model.User
import com.mad.carpooling.repository.UserRepository
import com.mad.carpooling.ui.EditProfileFragment
import com.mad.carpooling.ui.EditProfileFragmentDirections
import kotlinx.coroutines.launch

class EditProfileViewModel(private val userRepository: UserRepository) : ViewModel() {

    private lateinit var user: User

    fun getUser(): User {
        return user
    }

    fun setUser(user: User) {
        this.user = user
    }

    fun updateUser(newUser: User) : LiveData<Boolean>
    {
        val res = MutableLiveData<Boolean>()
        viewModelScope.launch {
            val result = userRepository.setUserDoc(newUser.uid, newUser)
            res.postValue(result)
        }
        return res
    }

}