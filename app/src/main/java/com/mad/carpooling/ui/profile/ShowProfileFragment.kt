package com.mad.carpooling.ui.profile

import android.content.Context
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
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.ui.onNavDestinationSelected
import com.google.android.material.navigation.NavigationView
import com.mad.carpooling.R
import org.json.JSONObject


class ShowProfileFragment : Fragment(R.layout.fragment_show_profile) {
    private lateinit var tvFullName: TextView
    private lateinit var tvNickname: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvLocation: TextView
    private lateinit var ivProfilePic: ImageView
    private lateinit var jsonObject: JSONObject
    private var currentPhotoPath: String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)

        tvFullName = view.findViewById<TextView>(R.id.tv_fullName)
        tvNickname = view.findViewById<TextView>(R.id.tv_nickname)
        tvEmail = view.findViewById<TextView>(R.id.tv_email)
        tvLocation = view.findViewById<TextView>(R.id.tv_location)
        ivProfilePic = view.findViewById<ImageView>(R.id.iv_profile_pic)

        initProfile(savedInstanceState)
    }

    private fun initProfile(savedInstanceState: Bundle?) {
        val args: ShowProfileFragmentArgs by navArgs()
        if (args.currentPhotoPath != null) {    // view created navigating from EditProfileFragment
            tvFullName.setText(args.fullname)
            tvNickname.setText(args.nickname)
            tvEmail.setText(args.email)
            tvLocation.setText(args.location)
            currentPhotoPath = args.currentPhotoPath
        } else {
            if (savedInstanceState == null) {   // view created for the first time
                val sharedPref =
                    context?.getSharedPreferences("profile_pref.group05.lab1", Context.MODE_PRIVATE)
                        ?: return
                val jsonString = sharedPref.getString(getString(R.string.saved_profile_data), null)
                if (jsonString != null) {
                    val jsonObject = JSONObject(jsonString)
                    tvFullName.text = jsonObject.getString("json_fullName.group05.lab1")
                    tvNickname.text = jsonObject.getString("json_nickname.group05.lab1")
                    tvEmail.text = jsonObject.getString("json_email.group05.lab1")
                    tvLocation.text = jsonObject.getString("json_location.group05.lab1")
                    currentPhotoPath = jsonObject.getString("json_profilePic.group05.lab1")
                }
            } else {                            // view created from state restore
                if (savedInstanceState.containsKey("state_currentPhoto")) {
                    currentPhotoPath = savedInstanceState.getString("state_currentPhoto")
                } else {
                    val sharedPref =
                        context?.getSharedPreferences(
                            "profile_pref.group05.lab1",
                            Context.MODE_PRIVATE
                        )
                            ?: return
                    val jsonString =
                        sharedPref.getString(getString(R.string.saved_profile_data), null)
                    if (jsonString != null) {
                        val jsonObject = JSONObject(jsonString)
                        currentPhotoPath = jsonObject.getString(
                            "json_profilePic.group05.lab1"
                        )
                    }
                }
            }
        }   // set img and init drawer
        BitmapFactory.decodeFile(currentPhotoPath)?.also { bitmap ->
            ivProfilePic.setImageBitmap(bitmap)
            initDrawerHeader(bitmap)
        }
    }


    private fun initDrawerHeader(bitmap: Bitmap) {
        val navView: NavigationView? = activity?.findViewById(R.id.nav_view)
        val headerView: View? = navView?.getHeaderView(0)
        val ivProfileHeader: ImageView? = headerView?.findViewById(R.id.nav_header_profile_pic)
        val tvFullNameHeader: TextView? = headerView?.findViewById(R.id.nav_header_fullName)
        val tvNicknameHeader: TextView? = headerView?.findViewById(R.id.nav_header_nickname)

        tvFullNameHeader?.text = tvFullName.text
        tvNicknameHeader?.text = tvNickname.text
        ivProfileHeader?.setImageBitmap(bitmap)

    }

    fun editProfile() {
        val action = ShowProfileFragmentDirections.actionNavShowProfileToNavEditProfile(
            tvFullName.text.toString(),
            tvNickname.text.toString(), tvEmail.text.toString(), tvLocation.text.toString()
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

    override fun onSaveInstanceState(outState: Bundle) {    //no need to save TextViews state because freezesText=true in xml layout
        if (currentPhotoPath != null) outState.putString("state_currentPhoto", currentPhotoPath)
    }

}