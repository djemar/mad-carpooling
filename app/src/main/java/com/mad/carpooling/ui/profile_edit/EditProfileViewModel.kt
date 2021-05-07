package com.mad.carpooling.ui.profile_edit

import androidx.lifecycle.ViewModel
import com.mad.carpooling.data.User

class EditProfileViewModel : ViewModel() {

    private lateinit var user: User

    fun getUser(): User {
        return user
    }

    fun setUser(user: User) {
        this.user = user
    }
}