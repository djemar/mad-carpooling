package com.mad.carpooling.ui.profile

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.navigation.NavigationView
import com.mad.carpooling.R
import org.json.JSONObject


class ShowProfileFragment : Fragment(R.layout.fragment_show_profile) {
    private lateinit var tvFullName: TextView
    private lateinit var tvNickname: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvLocation: TextView
    private lateinit var ivProfilePic: ImageView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)

        tvFullName = view.findViewById<TextView>(R.id.tv_fullName)
        tvNickname = view.findViewById<TextView>(R.id.tv_nickname)
        tvEmail = view.findViewById<TextView>(R.id.tv_email)
        tvLocation = view.findViewById<TextView>(R.id.tv_location)
        ivProfilePic = view.findViewById<ImageView>(R.id.iv_profile_pic)

        initProfile()
    }

    private fun initProfile() {
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
            val bitmap: Bitmap = BitmapFactory.decodeStream(
                activity?.openFileInput(
                    jsonObject.getString(
                        "json_profilePic.group05.lab1"
                    )
                )
            )
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
        val bundle = Bundle()

        bundle.putString("fullName.group05.lab1", tvFullName.text.toString())
        bundle.putString("nickname.group05.lab1", tvNickname.text.toString())
        bundle.putString("email.group05.lab1", tvEmail.text.toString())
        bundle.putString("location.group05.lab1", tvLocation.text.toString())

        findNavController().navigate(R.id.action_nav_show_profile_to_nav_edit_profile, bundle)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            val sharedPref =
                context?.getSharedPreferences("profile_pref.group05.lab1", Context.MODE_PRIVATE)
                    ?: return
            val jsonString = sharedPref.getString(getString(R.string.saved_profile_data), null)
            if (jsonString != null) {
                val jsonObject = JSONObject(jsonString)
                tvFullName.text = (data?.getStringExtra("save_fullName.group05.lab1"))
                tvNickname.text = (data?.getStringExtra("save_nickname.group05.lab1"))
                tvEmail.text = (data?.getStringExtra("save_email.group05.lab1"))
                tvLocation.text = (data?.getStringExtra("save_location.group05.lab1"))
                ivProfilePic.setImageBitmap(
                    BitmapFactory.decodeStream(
                        activity?.openFileInput(
                            jsonObject.getString(
                                "json_profilePic.group05.lab1"
                            )
                        )
                    )
                )
            }

        } /*else if (requestCode == 1 && resultCode == Activity.RESULT_CANCELED) { //debug
            Toast.makeText(this, "RESULT_CANCELED", Toast.LENGTH_SHORT).show()
        }*/
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.edit -> {
                editProfile()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }

    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_profile, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

}