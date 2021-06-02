package com.mad.carpooling.viewmodel

import androidx.lifecycle.ViewModel
import com.mad.carpooling.model.User

class EditProfileViewModel : ViewModel() {

    private lateinit var user: User

    fun getUser(): User {
        return user
    }

    fun setUser(user: User) {
        this.user = user
    }
}