package com.mad.carpooling.ui.profile

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.ui.onNavDestinationSelected
import com.bumptech.glide.Glide
import com.google.android.material.navigation.NavigationView
import com.mad.carpooling.R
import com.mad.carpooling.data.User
import com.mad.carpooling.ui.SharedViewModel


class ShowProfileFragment : Fragment(R.layout.fragment_show_profile) {
    private lateinit var tvFullName: TextView
    private lateinit var tvNickname: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvLocation: TextView
    private lateinit var ivProfilePic: ImageView
    private lateinit var user: User
    private var currentPhotoPath: String? = null
    private val model: SharedViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)

        tvFullName = view.findViewById(R.id.tv_fullName)
        tvNickname = view.findViewById(R.id.tv_nickname)
        tvEmail = view.findViewById(R.id.tv_email)
        tvLocation = view.findViewById(R.id.tv_location)
        ivProfilePic = view.findViewById(R.id.iv_profile_pic)

        //this works only for currentUser
        model.getCurrentUser().observe(viewLifecycleOwner, Observer { currentUser ->
            // Update the UI
            initProfile(currentUser)
        })
    }

    private fun initProfile(currentUser: User) {
        //val args: ShowProfileFragmentArgs by navArgs() needed only if supporting other profiles
        user = currentUser
        tvFullName.text = currentUser.fullname
        tvNickname.text = currentUser.nickname
        tvEmail.text = currentUser.email
        tvLocation.text = currentUser.location
        Glide.with(this).load(currentUser.imageUserRef).into(ivProfilePic)
    }

    private fun editProfile() {
        val action = ShowProfileFragmentDirections.actionNavShowProfileToNavEditProfile(
            user.uid
        )
        findNavController().navigate(action)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.nav_edit_profile -> {
                editProfile()
                true
            }
            else -> item.onNavDestinationSelected(findNavController()) || super.onOptionsItemSelected(
                item
            )
        }

    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_profile, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

}