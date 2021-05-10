package com.mad.carpooling.ui.profile

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.ui.onNavDestinationSelected
import com.bumptech.glide.Glide
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.mad.carpooling.MainActivity
import com.mad.carpooling.R
import com.mad.carpooling.data.User
import com.mad.carpooling.ui.SharedViewModel


class ShowProfileFragment : Fragment(R.layout.fragment_show_profile) {
    private lateinit var tvFullName: TextView
    private lateinit var tvNickname: TextView
    private lateinit var llEmail: LinearLayout
    private lateinit var tvEmail: TextView
    private lateinit var tvLocation: TextView
    private lateinit var ivProfilePic: ImageView
    private lateinit var user: User
    private var uid: String = ""
    private lateinit var optionsMenu: Menu
    private val model: SharedViewModel by activityViewModels()
    private val args: ShowProfileFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)

        tvFullName = view.findViewById(R.id.tv_fullName)
        tvNickname = view.findViewById(R.id.tv_nickname)
        llEmail = view.findViewById(R.id.title_email)
        tvEmail = view.findViewById(R.id.tv_email)
        tvLocation = view.findViewById(R.id.tv_location)
        ivProfilePic = view.findViewById(R.id.iv_profile_pic)

        val fab = (activity as MainActivity).findViewById<ExtendedFloatingActionButton>(R.id.fab)
        fab.hide()

        if (args.uid == "uid") {
            model.getCurrentUser().observe(viewLifecycleOwner, Observer { currentUser ->
                // Update the UI
                uid = currentUser.uid
                initProfile(currentUser)
            })
        } else {
            val db = Firebase.firestore
            db.collection("users").addSnapshotListener { value, e ->
                if (e != null) {
                    Log.e("loadUser() exception => ", e.toString())
                    return@addSnapshotListener
                }
                var tmpUser = User()
                for (doc in value!!) {
                    if (doc.id == args.uid) {
                        tmpUser = doc.toObject(User::class.java)
                        uid = tmpUser.uid
                    }
                }
                initProfile(tmpUser)
            }
        }
    }

    private fun initProfile(user: User) {
        this.user = user
        tvFullName.text = user.fullname
        tvNickname.text = user.nickname
        tvEmail.text = user.email
        tvLocation.text = user.location
        Glide.with(this).load(user.imageUserRef).into(ivProfilePic)
        llEmail.isVisible = uid == model.getCurrentUser().value?.uid
    }

    private fun editProfile() {
        val action = ShowProfileFragmentDirections.actionNavShowProfileToNavEditProfile(
            uid
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
        optionsMenu = menu
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        optionsMenu.findItem(R.id.nav_edit_profile).isVisible =
            uid == model.getCurrentUser().value?.uid
    }

}